package jp.ats.platemail.mail;

import jp.ats.platemail.common.Config;
import jp.ats.platemail.common.U;

public class AccountFinder {

	public static final String DELIVERED_TO_HEADER = "X-Original-To";

	/**
	 * メール内の情報から、配信先を検索し返す<br>
	 * ・配信先を特定するために、X-Original-Toヘッダに入っているアドレスを使用してRHKメールマスタの検索を行う<br>
	 * これは、メールサーバとしてpostfixを使用する前提の動作である（postfixがX-Original-Toヘッダを付与する仕様であるため）<br>
	 * X-Original-Toヘッダは、転送されてきたメールなどで重複することがあり得る<br>
	 * 重複している場合は、最前方にあるものが今回の配信先なので、それを使用する（postfixの場合、常に前方にX-Original-Toを付加するため）<br>
	 * ・X-Original-Toがない場合、TOまたはCCのアドレスを返す<br>
	 * X-Original-Toがない場合というのは、自身に宛ててのみメールを出した場合のことを想定している<br>
	 * その場合でも、TO、CCのどちらかに自身のアドレスが入っているはず
	 *
	 * @param source
	 * @return 配信先メールアドレス
	 */
	public static String findAddress(MessageDigest source) {
		AccountAddressDetector detector = U.getInstance(Config.getInstance().getAccountAddressDetectorClass());

		return detector.detect(
			source.getHeaderElements()
				.stream()
				.filter(h -> h.getHeader().equalsIgnoreCase(DELIVERED_TO_HEADER))
				.map(h -> h.getValue())
				.findFirst()//最前方のものを使用する
				.orElseGet(() -> findAlternateAddress(source))
				.trim());
	}

	/**
	 * Delivered-Toの代替としてTOがあればTOのアドレスを、なければCCのアドレスを返す
	 */
	private static String findAlternateAddress(MessageDigest source) {
		return source.getRecipients()
			.stream()
			.filter(r -> r.getType().equals(javax.mail.Message.RecipientType.TO))
			.findFirst()
			.orElseGet(
				() -> source.getRecipients()
					.stream()
					.filter(r -> r.getType().equals(javax.mail.Message.RecipientType.CC))
					.findFirst()
					.get())
			.getAddress();
	}
}
