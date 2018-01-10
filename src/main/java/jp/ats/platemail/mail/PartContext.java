package jp.ats.platemail.mail;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.Part;

import jp.ats.platemail.U;

enum PartContext {

	//通常（Multipartではない場合）
	Plain {

		@Override
		void processTextBody(MessageParser parser, Part part, PartListener listener) throws IOException, MessagingException {
			parser.processTextBody(part, listener);
		}

		@Override
		void processAttachment(MessageParser parser, Part part, PartListener listener) throws IOException, MessagingException {
			//想定外のmime typeなので、添付ファイルとして保存しておく
			parser.processAttachment(part, listener);
		}
	},

	//multipart-mixed以下の処理
	MultipartMixed {

		@Override
		void processTextBody(MessageParser parser, Part part, PartListener listener) throws IOException, MessagingException {
			//ファイル名がある場合は添付ファイルとみなす
			if (U.presents(part.getFileName())) {
				parser.processAttachment(part, listener);
				return;
			}

			//既に本文が出現している場合は添付ファイルとみなす
			if (parser.alreadyHasTextBody()) {
				parser.processAttachment(part, listener);
				return;
			}

			//ファイル名を持たず、まだ本文が出現していない場合のみ本文とする
			parser.processTextBody(part, listener);
		}

		@Override
		void processAttachment(MessageParser parser, Part part, PartListener listener) throws IOException, MessagingException {
			parser.processAttachment(part, listener);
		}
	},

	//multipart-alternative以下の処理
	MultipartAlternative {

		@Override
		void processTextBody(MessageParser parser, Part part, PartListener listener) throws IOException, MessagingException {
			if (U.presents(part.getFileName())) {
				//ファイル名がある場合は添付ファイルとみなす
				//HTMLメールがmultipart/relatedの場合で、テキストファイルが添付されていた場合
				parser.processAttachment(part, listener);
				return;
			}

			parser.processTextBody(part, listener);
		}

		@Override
		void processAttachment(MessageParser parser, Part part, PartListener listener) throws IOException, MessagingException {
			//HTMLメールがmultipart/relatedの場合の添付ファイル
			parser.processAttachment(part, listener);
		}
	};

	abstract void processTextBody(MessageParser parser, Part part, PartListener listener) throws IOException, MessagingException;

	abstract void processAttachment(MessageParser parser, Part part, PartListener listener) throws IOException, MessagingException;
}
