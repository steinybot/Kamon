package kamon.context.encoding;


// Code generated by colf(1); DO NOT EDIT.


import static java.lang.String.format;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.InputMismatchException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;


/**
 * Data bean with built-in serialization support.

 * @author generated by colf(1)
 * @see <a href="https://github.com/pascaldekloe/colfer">Colfer's home</a>
 */
@javax.annotation.Generated(value="colf(1)", comments="Colfer from schema file context.colf")
public class Entry implements Serializable {

	/** The upper limit for serial byte sizes. */
	public static int colferSizeMax = 16 * 1024 * 1024;




	public String name;

	public byte[] content;


	/** Default constructor */
	public Entry() {
		init();
	}

	private static final byte[] _zeroBytes = new byte[0];

	/** Colfer zero values. */
	private void init() {
		name = "";
		content = _zeroBytes;
	}

	/**
	 * {@link #reset(InputStream) Reusable} deserialization of Colfer streams.
	 */
	public static class Unmarshaller {

		/** The data source. */
		protected InputStream in;

		/** The read buffer. */
		public byte[] buf;

		/** The {@link #buf buffer}'s data start index, inclusive. */
		protected int offset;

		/** The {@link #buf buffer}'s data end index, exclusive. */
		protected int i;


		/**
		 * @param in the data source or {@code null}.
		 * @param buf the initial buffer or {@code null}.
		 */
		public Unmarshaller(InputStream in, byte[] buf) {
			// TODO: better size estimation
			if (buf == null || buf.length == 0)
				buf = new byte[Math.min(Entry.colferSizeMax, 2048)];
			this.buf = buf;
			reset(in);
		}

		/**
		 * Reuses the marshaller.
		 * @param in the data source or {@code null}.
		 * @throws IllegalStateException on pending data.
		 */
		public void reset(InputStream in) {
			if (this.i != this.offset) throw new IllegalStateException("colfer: pending data");
			this.in = in;
			this.offset = 0;
			this.i = 0;
		}

		/**
		 * Deserializes the following object.
		 * @return the result or {@code null} when EOF.
		 * @throws IOException from the input stream.
		 * @throws SecurityException on an upper limit breach defined by {@link #colferSizeMax}.
		 * @throws InputMismatchException when the data does not match this object's schema.
		 */
		public Entry next() throws IOException {
			if (in == null) return null;

			while (true) {
				if (this.i > this.offset) {
					try {
						Entry o = new Entry();
						this.offset = o.unmarshal(this.buf, this.offset, this.i);
						return o;
					} catch (BufferUnderflowException e) {
					}
				}
				// not enough data

				if (this.i <= this.offset) {
					this.offset = 0;
					this.i = 0;
				} else if (i == buf.length) {
					byte[] src = this.buf;
					// TODO: better size estimation
					if (offset == 0) this.buf = new byte[Math.min(Entry.colferSizeMax, this.buf.length * 4)];
					System.arraycopy(src, this.offset, this.buf, 0, this.i - this.offset);
					this.i -= this.offset;
					this.offset = 0;
				}
				assert this.i < this.buf.length;

				int n = in.read(buf, i, buf.length - i);
				if (n < 0) {
					if (this.i > this.offset)
						throw new InputMismatchException("colfer: pending data with EOF");
					return null;
				}
				assert n > 0;
				i += n;
			}
		}

	}


	/**
	 * Serializes the object.
	 * @param out the data destination.
	 * @param buf the initial buffer or {@code null}.
	 * @return the final buffer. When the serial fits into {@code buf} then the return is {@code buf}.
	 *  Otherwise the return is a new buffer, large enough to hold the whole serial.
	 * @throws IOException from {@code out}.
	 * @throws IllegalStateException on an upper limit breach defined by {@link #colferSizeMax}.
	 */
	public byte[] marshal(OutputStream out, byte[] buf) throws IOException {
		// TODO: better size estimation
		if (buf == null || buf.length == 0)
			buf = new byte[Math.min(Entry.colferSizeMax, 2048)];

		while (true) {
			int i;
			try {
				i = marshal(buf, 0);
			} catch (BufferOverflowException e) {
				buf = new byte[Math.min(Entry.colferSizeMax, buf.length * 4)];
				continue;
			}

			out.write(buf, 0, i);
			return buf;
		}
	}

	/**
	 * Serializes the object.
	 * @param buf the data destination.
	 * @param offset the initial index for {@code buf}, inclusive.
	 * @return the final index for {@code buf}, exclusive.
	 * @throws BufferOverflowException when {@code buf} is too small.
	 * @throws IllegalStateException on an upper limit breach defined by {@link #colferSizeMax}.
	 */
	public int marshal(byte[] buf, int offset) {
		int i = offset;

		try {
			if (! this.name.isEmpty()) {
				buf[i++] = (byte) 0;
				int start = ++i;

				String s = this.name;
				for (int sIndex = 0, sLength = s.length(); sIndex < sLength; sIndex++) {
					char c = s.charAt(sIndex);
					if (c < '\u0080') {
						buf[i++] = (byte) c;
					} else if (c < '\u0800') {
						buf[i++] = (byte) (192 | c >>> 6);
						buf[i++] = (byte) (128 | c & 63);
					} else if (c < '\ud800' || c > '\udfff') {
						buf[i++] = (byte) (224 | c >>> 12);
						buf[i++] = (byte) (128 | c >>> 6 & 63);
						buf[i++] = (byte) (128 | c & 63);
					} else {
						int cp = 0;
						if (++sIndex < sLength) cp = Character.toCodePoint(c, s.charAt(sIndex));
						if ((cp >= 1 << 16) && (cp < 1 << 21)) {
							buf[i++] = (byte) (240 | cp >>> 18);
							buf[i++] = (byte) (128 | cp >>> 12 & 63);
							buf[i++] = (byte) (128 | cp >>> 6 & 63);
							buf[i++] = (byte) (128 | cp & 63);
						} else
							buf[i++] = (byte) '?';
					}
				}
				int size = i - start;
				if (size > Entry.colferSizeMax)
					throw new IllegalStateException(format("colfer: kamon/context/kamon.Entry.name size %d exceeds %d UTF-8 bytes", size, Entry.colferSizeMax));

				int ii = start - 1;
				if (size > 0x7f) {
					i++;
					for (int x = size; x >= 1 << 14; x >>>= 7) i++;
					System.arraycopy(buf, start, buf, i - size, size);

					do {
						buf[ii++] = (byte) (size | 0x80);
						size >>>= 7;
					} while (size > 0x7f);
				}
				buf[ii] = (byte) size;
			}

			if (this.content.length != 0) {
				buf[i++] = (byte) 1;

				int size = this.content.length;
				if (size > Entry.colferSizeMax)
					throw new IllegalStateException(format("colfer: kamon/context/kamon.Entry.content size %d exceeds %d bytes", size, Entry.colferSizeMax));

				int x = size;
				while (x > 0x7f) {
					buf[i++] = (byte) (x | 0x80);
					x >>>= 7;
				}
				buf[i++] = (byte) x;

				int start = i;
				i += size;
				System.arraycopy(this.content, 0, buf, start, size);
			}

			buf[i++] = (byte) 0x7f;
			return i;
		} catch (ArrayIndexOutOfBoundsException e) {
			if (i - offset > Entry.colferSizeMax)
				throw new IllegalStateException(format("colfer: kamon/context/kamon.Entry exceeds %d bytes", Entry.colferSizeMax));
			if (i > buf.length) throw new BufferOverflowException();
			throw e;
		}
	}

	/**
	 * Deserializes the object.
	 * @param buf the data source.
	 * @param offset the initial index for {@code buf}, inclusive.
	 * @return the final index for {@code buf}, exclusive.
	 * @throws BufferUnderflowException when {@code buf} is incomplete. (EOF)
	 * @throws SecurityException on an upper limit breach defined by {@link #colferSizeMax}.
	 * @throws InputMismatchException when the data does not match this object's schema.
	 */
	public int unmarshal(byte[] buf, int offset) {
		return unmarshal(buf, offset, buf.length);
	}

	/**
	 * Deserializes the object.
	 * @param buf the data source.
	 * @param offset the initial index for {@code buf}, inclusive.
	 * @param end the index limit for {@code buf}, exclusive.
	 * @return the final index for {@code buf}, exclusive.
	 * @throws BufferUnderflowException when {@code buf} is incomplete. (EOF)
	 * @throws SecurityException on an upper limit breach defined by {@link #colferSizeMax}.
	 * @throws InputMismatchException when the data does not match this object's schema.
	 */
	public int unmarshal(byte[] buf, int offset, int end) {
		if (end > buf.length) end = buf.length;
		int i = offset;

		try {
			byte header = buf[i++];

			if (header == (byte) 0) {
				int size = 0;
				for (int shift = 0; true; shift += 7) {
					byte b = buf[i++];
					size |= (b & 0x7f) << shift;
					if (shift == 28 || b >= 0) break;
				}
				if (size < 0 || size > Entry.colferSizeMax)
					throw new SecurityException(format("colfer: kamon/context/kamon.Entry.name size %d exceeds %d UTF-8 bytes", size, Entry.colferSizeMax));

				int start = i;
				i += size;
				this.name = new String(buf, start, size, StandardCharsets.UTF_8);
				header = buf[i++];
			}

			if (header == (byte) 1) {
				int size = 0;
				for (int shift = 0; true; shift += 7) {
					byte b = buf[i++];
					size |= (b & 0x7f) << shift;
					if (shift == 28 || b >= 0) break;
				}
				if (size < 0 || size > Entry.colferSizeMax)
					throw new SecurityException(format("colfer: kamon/context/kamon.Entry.content size %d exceeds %d bytes", size, Entry.colferSizeMax));

				this.content = new byte[size];
				int start = i;
				i += size;
				System.arraycopy(buf, start, this.content, 0, size);

				header = buf[i++];
			}

			if (header != (byte) 0x7f)
				throw new InputMismatchException(format("colfer: unknown header at byte %d", i - 1));
		} finally {
			if (i > end && end - offset < Entry.colferSizeMax) throw new BufferUnderflowException();
			if (i < 0 || i - offset > Entry.colferSizeMax)
				throw new SecurityException(format("colfer: kamon/context/kamon.Entry exceeds %d bytes", Entry.colferSizeMax));
			if (i > end) throw new BufferUnderflowException();
		}

		return i;
	}

	// {@link Serializable} version number.
	private static final long serialVersionUID = 2L;

	// {@link Serializable} Colfer extension.
	private void writeObject(ObjectOutputStream out) throws IOException {
		// TODO: better size estimation
		byte[] buf = new byte[1024];
		int n;
		while (true) try {
			n = marshal(buf, 0);
			break;
		} catch (BufferUnderflowException e) {
			buf = new byte[4 * buf.length];
		}

		out.writeInt(n);
		out.write(buf, 0, n);
	}

	// {@link Serializable} Colfer extension.
	private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
		init();

		int n = in.readInt();
		byte[] buf = new byte[n];
		in.readFully(buf);
		unmarshal(buf, 0);
	}

	// {@link Serializable} Colfer extension.
	private void readObjectNoData() throws ObjectStreamException {
		init();
	}

	/**
	 * Gets kamon/context/kamon.Entry.name.
	 * @return the value.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets kamon/context/kamon.Entry.name.
	 * @param value the replacement.
	 */
	public void setName(String value) {
		this.name = value;
	}

	/**
	 * Sets kamon/context/kamon.Entry.name.
	 * @param value the replacement.
	 * @return {link this}.
	 */
	public Entry withName(String value) {
		this.name = value;
		return this;
	}

	/**
	 * Gets kamon/context/kamon.Entry.content.
	 * @return the value.
	 */
	public byte[] getContent() {
		return this.content;
	}

	/**
	 * Sets kamon/context/kamon.Entry.content.
	 * @param value the replacement.
	 */
	public void setContent(byte[] value) {
		this.content = value;
	}

	/**
	 * Sets kamon/context/kamon.Entry.content.
	 * @param value the replacement.
	 * @return {link this}.
	 */
	public Entry withContent(byte[] value) {
		this.content = value;
		return this;
	}

	@Override
	public final int hashCode() {
		int h = 1;
		if (this.name != null) h = 31 * h + this.name.hashCode();
		for (byte b : this.content) h = 31 * h + b;
		return h;
	}

	@Override
	public final boolean equals(Object o) {
		return o instanceof Entry && equals((Entry) o);
	}

	public final boolean equals(Entry o) {
		if (o == null) return false;
		if (o == this) return true;
		return o.getClass() == Entry.class
			&& (this.name == null ? o.name == null : this.name.equals(o.name))
			&& java.util.Arrays.equals(this.content, o.content);
	}

}
