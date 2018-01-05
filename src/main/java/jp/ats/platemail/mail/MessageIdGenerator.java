package jp.ats.platemail.mail;

import jp.ats.platemail.common.Config;
import jp.ats.platemail.common.U;

public abstract class MessageIdGenerator {

	public static MessageIdGenerator getInstance() {
		return U.getInstance(Config.getInstance().getMessageIdGeneratorClass());
	}

	public abstract String generate();
}
