package jp.ats.platemail.account.vam;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Virtual Alias Map更新時の結果を保持するクラス
 * ドメインごとの件数をメッセージとして生成する
 */
public class VamResult {

	private final Map<String, Integer> map = new TreeMap<>();

	VamResult() {}

	void incrementAt(String domain) {
		Integer count = map.get(domain);
		map.put(domain, (count == null ? 0 : count) + 1);
	}

	public String getResultMessage() {
		List<String> results = new LinkedList<>();
		map.forEach((domain, accounts) -> results.add(domain + " : " + accounts + " 件"));
		return String.join(", ", results);
	}
}
