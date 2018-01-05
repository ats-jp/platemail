package jp.ats.platemail.account.filter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MessageTruncatorTest {

	@Test
	public void getBodySizeTest() {
		byte[] data = {};
		assertEquals(0, MessageTruncator.getBodySize(data));

		data = "header\r\nbody".getBytes();
		assertEquals(0, MessageTruncator.getBodySize(data));

		data = "header\r\n\r\nbody".getBytes();
		assertEquals(4, MessageTruncator.getBodySize(data));

		data = "header\r\n\r\nbody\r\nbody".getBytes();
		assertEquals(10, MessageTruncator.getBodySize(data));

		data = "header\n\nbody".getBytes();
		assertEquals(4, MessageTruncator.getBodySize(data));

		data = "header\r\rbody".getBytes();
		assertEquals(4, MessageTruncator.getBodySize(data));

		data = "header\r\n\rbody".getBytes();
		assertEquals(4, MessageTruncator.getBodySize(data));
	}
}
