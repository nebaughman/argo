package net.nyhm.argo

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Extension property that provides a [Logger] for the given object's class.
 * Notice that extension properties are evaluated statically, so it's the declared
 * type of the object's variable that is used here, not the runtime type of the object.
 * This is a useful behavior for logging, because it can be more useful to label log events
 * from the immediate class where the log is generated than the runtime type of the object.
 *
 * Notice that this can be used by calling `this.log.info(..)` or directly called upon
 * an object `someObject.log.info(..)`.
 */
val Any?.log
  get() = if (this == null)
    net.nyhm.argo.log
  else
    LoggerFactory.getLogger(this::class.java)

/**
 * A [Logger] instance that can be used universally.
 * Prefer the [log] extension, as it includes the source of the log.
 */
val log: Logger = LoggerFactory.getLogger("argo")
