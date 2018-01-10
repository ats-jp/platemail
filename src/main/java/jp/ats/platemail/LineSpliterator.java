package jp.ats.platemail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class LineSpliterator {

	private ByteBuffer buffer;

	public LineSpliterator() {
		buffer = ByteBuffer.allocate(1024);
	}

	public LineSpliterator(int bufferCapacity) {
		buffer = ByteBuffer.allocate(bufferCapacity);
	}

	public void split(InputStream input, LineConsumer consumer) throws IOException {
		input = U.wrap(input);

		Runnable runner = () -> {
			consumer.accept(buffer.array(), buffer.position());
			buffer.position(0);
		};

		try {
			Status status = Status.OTHER;
			int b;
			while ((b = input.read()) != -1) {
				status = status.apply(b, runner);

				if (buffer.remaining() == 0) ensure();

				buffer.put((byte) b);
			}

			runner.run();
		} catch (Brake brake) {
			return;
		}
	}

	private void ensure() {
		ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
		newBuffer.put(buffer.array(), 0, buffer.position());
		buffer = newBuffer;
	}

	@FunctionalInterface
	public interface LineConsumer {

		void accept(byte[] buffer, int length);
	}

	@SuppressWarnings("serial")
	public static class Brake extends RuntimeException {}

	private enum Status {

		OTHER {

			@Override
			Status apply(int next, Runnable lineRunner) {
				switch (next) {
				case '\r':
					return CR;
				case '\n':
					return LF;
				default:
					return OTHER;
				}
			}
		},

		CR {

			@Override
			Status apply(int next, Runnable lineRunner) {
				switch (next) {
				case '\r':
					lineRunner.run();
					return CR;
				case '\n':
					return LF;
				default:
					lineRunner.run();
					return OTHER;
				}
			}
		},

		LF {

			@Override
			Status apply(int next, Runnable lineRunner) {
				lineRunner.run();
				return OTHER.apply(next, lineRunner);
			}
		};

		abstract Status apply(int b, Runnable lineRunner);
	}
}
