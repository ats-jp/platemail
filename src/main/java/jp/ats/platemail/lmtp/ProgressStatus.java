package jp.ats.platemail.lmtp;

import java.io.IOException;

enum ProgressStatus {

	//header処理中
	HEADER_LINE {

		@Override
		ProgressStatus accept(ProgressAgent agent, byte[] bytes, int length) throws IOException {
			//CRLFのみの行（空行）でheaderの終わりを判定
			if (length == 2 && bytes[0] == '\r' && bytes[1] == '\n') {
				agent.startCommunicate();

				//全header write
				agent.flushHeaders();
				//区切りの空行 write
				agent.sendServer(bytes, length);

				return BODY_LINE;
			}

			//header処理中
			//まだ区切りの空行に到達していない
			agent.addHeader(bytes, length);

			return this;
		}
	},

	//body処理中
	BODY_LINE {

		@Override
		ProgressStatus accept(ProgressAgent agent, byte[] bytes, int length) throws IOException {
			//body内の行頭の . は、 .. に変換
			if (bytes[0] == '.') agent.sendServer(dot, 1);
			agent.sendServer(bytes, length);

			agent.setEndsWithCrlf(length > 2 && bytes[length - 1] == '\n' && bytes[length - 2] == '\r');

			return this;
		}
	};

	private static final byte[] dot = { '.' };

	//行を処理し、次に適切なstatusを返す
	abstract ProgressStatus accept(ProgressAgent agent, byte[] bytes, int length) throws IOException;
}
