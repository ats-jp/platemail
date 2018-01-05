package jp.ats.platemail.account;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.ats.platemail.common.Config;
import jp.ats.platemail.common.U;

public class Domain implements Comparable<Domain> {

	private final String name;

	private final String alias;

	private static final Pattern pattern = Pattern.compile("([^\\s]+)\\s+\\->\\s+([^\\s]+)");

	//key:destination, value:alias となるMap
	static Map<String, String> createDestinationAliasMap(Config config) {
		Map<String, String> map = new HashMap<>();
		executeForAliasDomains(config, matcher -> map.put(matcher.group(2), matcher.group(1)));
		return map;
	}

	//key:alias, value:destination となるMap
	static Map<String, String> createAliasDestinationMap(Config config) {
		Map<String, String> map = new HashMap<>();
		executeForAliasDomains(config, matcher -> map.put(matcher.group(1), matcher.group(2)));
		return map;
	}

	private static void executeForAliasDomains(Config config, Consumer<Matcher> consumer) {
		String domainAliases = config.getDomainAliases();

		//設定がない場合
		if (!U.presents(domainAliases)) return;

		U.splitByComma(domainAliases).stream().map(U::removeWhiteSpaces).forEach(nameAndAlias -> {
			Matcher matcher = pattern.matcher(nameAndAlias);
			if (!matcher.find()) throw new IllegalArgumentException(nameAndAlias);
			consumer.accept(matcher);
		});
	}

	Domain(String name, String alias) {
		Objects.requireNonNull(name);
		this.name = name;
		this.alias = alias;
	}

	public String name() {
		return name;
	}

	public boolean hasAlias() {
		return U.presents(alias);
	}

	public String alias() {
		return alias;
	}

	public String getViewString() {
		return hasAlias() ? name + " (" + alias + ")" : name;
	}

	@Override
	public boolean equals(Object another) {
		if (!(another instanceof Domain)) return false;
		return name.equals(((Domain) another).name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public int compareTo(Domain another) {
		return name.compareTo(another.name);
	}
}
