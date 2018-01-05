package jp.ats.platemail.fcm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NotificatorStub implements Notificator {

	private static final Logger logger = LogManager.getLogger(NotificatorStub.class.getName());

	@Override
	public Response request(String serverKey, String requestJson) {
		logger.info("request: " + requestJson);
		return new Response(200, "");
	}
}
