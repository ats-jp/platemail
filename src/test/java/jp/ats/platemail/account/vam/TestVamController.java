package jp.ats.platemail.account.vam;

public class TestVamController implements VamController {

	@Override
	public void add(String address) {
		System.out.println("address: [" + address + "]");
	}

	@Override
	public VamResult commit() {
		return new VamResult();
	}
}
