package jp.ats.platemail.account.vam;

public class VamControllerStub implements VamController {

	@Override
	public void add(String address) {}

	@Override
	public VamResult commit() {
		return new VamResult();
	}
}
