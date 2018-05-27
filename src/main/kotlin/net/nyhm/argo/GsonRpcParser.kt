package net.nyhm.argo

import com.google.gson.*
import java.lang.reflect.Type

/**
 * A [Gson] implementation of an [RpcParser].
 *
 * Create a [GsonRpcParser] with a [Gson] paramsParser, which is responsible for
 * parsing any [RpcParams] types that may be requested by the user.
 *
 * If no custom parameters are expected (eg, only params handled natively by Gson),
 * then the default parameter can be used.
 */
class GsonRpcParser(paramsParser: Gson = Gson()): RpcParser<String>
{
  private val gson = GsonBuilder()
      .registerTypeAdapter(RpcRequest::class.java, RpcRequestDeserializer(paramsParser))
      .registerTypeAdapter(RpcResponse::class.java, RpcResponseSerializer(paramsParser))
      .create()

  override fun parseRequest(json: String): RpcRequest = gson.fromJson(json, RpcRequest::class.java)

  override fun export(response: RpcResponse): String = gson.toJson(response)
}

private class RpcResponseSerializer(
    val paramsParser: Gson
): JsonSerializer<RpcResponse> {
  override fun serialize(src: RpcResponse, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
    val map = mutableMapOf(
        "jsonrpc" to RPC_VERSION_2_0.version,
        "id" to src.id.id
    )
    if (src.result != null) {
      map["result"] = paramsParser.toJsonTree(src.result.result)
    }
    // TODO: verify that exactly one result|error exists
    if (src.error != null) {
      map["error"] = paramsParser.toJsonTree(src.error.message)
    }
    return paramsParser.toJsonTree(map)
  }
}

/**
 * This class will deserialize [RpcRequest] messages
 */
private class RpcRequestDeserializer(
  val paramsParser: Gson
): JsonDeserializer<RpcRequest>
{
  override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): RpcRequest {
    if (!json.isJsonObject) throw RpcParseError("Request is not json object", json = json)
    val req = json.asJsonObject
    val version = RpcVersion(req.getAsJsonPrimitive("jsonrpc").asString)
    if (version != RPC_VERSION_2_0) throw RpcParseError("Unexpected version", json = json)
    val method = RpcMethod(req.getAsJsonPrimitive("method").asString)
    val paramsJson = req.get("params")
    val params = if (paramsJson == null || paramsJson.isJsonNull) {
      null // params are optional
    } else if (paramsJson.isJsonArray || paramsJson.isJsonObject) {
      GsonParams(paramsParser, paramsJson)
    } else {
      throw RpcParseError("Invalid json params")
    }
    val idJson = req.get("id")
    val id = if (idJson == null || idJson.isJsonNull) {
      null // id is optional
    } else {
      RpcId(idJson)
    }
    return RpcRequest(version, method, params, id)
  }
}

/**
 * This class can parse Json-Rpc request params with a user-specified parser.
 */
private class GsonParams(
    private val parser: Gson,
    private val data: JsonElement
): RpcParams
{
  override fun isPositional() = data.isJsonArray

  override fun <T> get(index: Int, type: Class<T>): T? {
    if (!isPositional()) throw RpcParseError("Cannot request positional params of named params")
    return parse(data.asJsonArray[index], type)
  }

  override fun <T> get(name: String, type: Class<T>): T? {
    if (isPositional()) throw RpcParseError("Cannot request named params of positional params")
    return parse(data.asJsonObject.get(name), type)
  }

  private fun <T> parse(json: JsonElement, type: Class<T>): T? {
    return parser.fromJson(json, type)
  }
}