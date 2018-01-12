package jp.ats.platemail;

import org.apache.logging.log4j.Logger;

class ProcessUtils {

	static RuntimeException error(Logger logger, String message, Throwable t) {
		logger.error(message, t);
		return new RuntimeException(message, t);
	}
}
