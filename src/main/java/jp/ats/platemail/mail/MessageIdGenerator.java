package jp.ats.platemail.mail;

import jp.ats.platemail.Config;
import jp.ats.platemail.U;

public abstract class MessageIdGenerator {

	public static MessageIdGenerator getInstance() {
		return U.getInstance(Config.getInstance().getMessageIdGeneratorClass());
	}

	public abstract String generate();
}
