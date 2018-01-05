package jp.ats.platemail.mail;

public interface Listener {

	default void start() {}

	default void end() {}

	default void notifyHeader(String header, String value) {};
}
