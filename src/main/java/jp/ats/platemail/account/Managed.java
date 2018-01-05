package jp.ats.platemail.account;

public interface Managed extends Identified {

	long getRevision();

	void updateRevision();

	Status getStatus();
}
