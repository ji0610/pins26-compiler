package pins26.phase;

import java.io.*;

import pins26.common.*;

/**
 * Leksikalni analizator.
 */
public class LexAn implements AutoCloseable {

	/** Izvorna datoteka. */
	private final Reader srcFile;

	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public LexAn(final String srcFileName) {
		try {
			srcFile = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srcFileName))));
			nextChar(); // Pripravi prvi znak izvorne datoteke (glej {@link nextChar}).
		} catch (FileNotFoundException __) {
			throw new Report.Error("Source file '" + srcFileName + "' not found.");
		}
	}

	@Override
	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file.");
		}
	}

	/** Trenutni znak izvorne datoteke (glej {@link nextChar}). */
	private int buffChar = -2;

	/** Vrstica trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharLine = 0;

	/** Stolpec trenutnega znaka izvorne datoteke (glej {@link nextChar}). */
	private int buffCharColumn = 0;

	/**
	 * Prebere naslednji znak izvorne datoteke.
	 * 
	 * Izvorno datoteko beremo znak po znak. Trenutni znak izvorne datoteke je
	 * shranjen v spremenljivki {@link buffChar}, vrstica in stolpec trenutnega
	 * znaka izvorne datoteke sta shranjena v spremenljivkah {@link buffCharLine} in
	 * {@link buffCharColumn}.
	 * 
	 * Zacetne vrednosti {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} so {@code '\n'}, {@code 0} in {@code 0}: branje prvega
	 * znaka izvorne datoteke bo na osnovi vrednosti {@code '\n'} spremenljivke
	 * {@link buffChar} prvemu znaku izvorne datoteke priredilo vrstico 1 in stolpec
	 * 1.
	 * 
	 * Pri branju izvorne datoteke se predpostavlja, da je v spremenljivki
	 * {@link buffChar} ves "cas veljaven znak. Zunaj metode {@link nextChar} so vse
	 * spremenljivke {@link buffChar}, {@link buffCharLine} in
	 * {@link buffCharColumn} namenjene le branju.
	 * 
	 * Vrednost {@code -1} v spremenljivki {@link buffChar} pomeni konec datoteke
	 * (vrednosti spremenljivk {@link buffCharLine} in {@link buffCharColumn} pa
	 * nista ve"c veljavni).
	 */
	private void nextChar() {
		try {
			switch (buffChar) {
			case -2: // Noben znak "se ni bil prebran.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? 0 : 1;
				buffCharColumn = buffChar == -1 ? 0 : 1;
				return;
			case -1: // Konec datoteke je bil "ze viden.
				return;
			case '\n': // Prejsnji znak je koncal vrstico, zacne se nova vrstica.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? buffCharLine : buffCharLine + 1;
				buffCharColumn = buffChar == -1 ? buffCharColumn : 1;
				return;
			case '\t': // Prejsnji znak je tabulator, ta znak je morda potisnjen v desno.
				buffChar = srcFile.read();
				while (buffCharColumn % 8 != 0)
					buffCharColumn += 1;
				buffCharColumn += 1;
				return;
			default: // Prejsnji znak je brez posebnosti.
				buffChar = srcFile.read();
				buffCharColumn += 1;
				return;
			}
		} catch (IOException __) {
			throw new Report.Error("Cannot read source file.");
		}
	}

	/**
	 * Trenutni leksikalni simbol.
	 * 
	 * "Ce vrednost spremenljivke {@code buffToken} ni {@code null}, je simbol "ze
	 * prebran iz vhodne datoteke, ni pa "se predan naprej sintaksnemu analizatorju.
	 * Ta simbol je dostopen z metodama {@link peekToken} in {@link takeToken}.
	 */
	private Token buffToken = null;

	/**
	 * Prebere naslednji leksikalni simbol, ki je nato dostopen preko metod
	 * {@link peekToken} in {@link takeToken}.
	 */
	private void nextToken() {

		StringBuilder sb = new StringBuilder();

		while (buffChar == ' ' || buffChar == '\n' || buffChar == '\t' || buffChar == '\r') {
			nextChar();
		}

		if (buffChar == -1) {
			buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn, buffCharLine, buffCharColumn), Token.Symbol.EOF, "");
			return;
		}

		int startCharLine = buffCharLine;
		int startCharCol = buffCharColumn;

		// INTCONST
		if (Character.isDigit(buffChar)) {
			sb.append((char)buffChar);
			if (buffChar == '0') {
				nextChar();
				buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.INTCONST, sb.toString());
				return;
			}
			nextChar();
			while (Character.isDigit(buffChar)) {
				sb.append((char)buffChar);
				nextChar();
			}
			buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.INTCONST, sb.toString());
			return;

		// CHARCONST
		} else if (buffChar == '\'') {
			nextChar();

			// posebni primeri
			if (buffChar == '\\') {
				nextChar();

				// '\n'
				if (buffChar == 'n') {
					sb.append('\n');
					nextChar();
				// '\'
				} else if (buffChar == '\\') {
					sb.append('\\');
					nextChar();
				// '''
				} else if (buffChar == '\'') {
					sb.append('\'');
					nextChar();
				// HEX '\xx'
				} else if ((buffChar >= '0' && buffChar <= '9' ) || (buffChar >= 'a' && buffChar <= 'f' )){

					int value = Character.digit(buffChar, 16) * 16;
					nextChar();
					if ((buffChar >= '0' && buffChar <= '9' ) || (buffChar >= 'a' && buffChar <= 'f' )){

						value += Character.digit(buffChar, 16);
						sb.append((char) value);

						nextChar();
					} else {
						throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Neveljavna šestnajstiška številka");
					}
				} else {
					throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Neveljavna šestnajstiška številka");
				}

			// prepreči prazen char
			} else if (buffChar >= 32 && buffChar <= 126 && buffChar != '\'') {
				sb.append((char)buffChar);
				nextChar();
			} else {
				throw new Report.Error(new Report.Location(startCharLine, startCharCol), "Napaka, prazen char");
			}

			if (buffChar == '\'') {
				nextChar(); // Consume the closing '
				buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.CHARCONST, sb.toString());
				return;
			} else {
				throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Manjka enojna navednica");
			}


		// STRINGCONST
		} else if (buffChar == '\"') {
			nextChar();

			while (buffChar != '\"' && buffChar != -1) {

				if (buffChar == '\\') {
					nextChar();

					if (buffChar == 'n') {
						sb.append('\n');
						nextChar();

					} else if (buffChar == '\\') {
						sb.append('\\');
						nextChar();

					} else if (buffChar == '\"') {
						sb.append('\"');
						nextChar();

					} else if ((buffChar >= '0' && buffChar <= '9' ) || (buffChar >= 'a' && buffChar <= 'f' )){

						int value = Character.digit(buffChar, 16) * 16;
						nextChar();
						if ((buffChar >= '0' && buffChar <= '9' ) || (buffChar >= 'a' && buffChar <= 'f' )){

							value += Character.digit(buffChar, 16);
							sb.append((char) value);

							nextChar();
						} else {
							throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Neveljavna šestnajstiška številka");
						}
					} else {
						throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Neveljavna šestnajstiška številka");
					}
				} else if (buffChar >= 32 && buffChar <= 126) {
					sb.append((char)buffChar);
					nextChar();
				} else {
					throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Neveljaven znak v nizu");
				}
			}

			// EOF preden se String zaključi
			if (buffChar == -1) {
				throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Manjkajo dvojne navednice");
			}

			if (buffChar == '\"') {
				nextChar();
				buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.STRINGCONST, sb.toString());
				return;
			}


		// IDENTIFIER
		} else if ((buffChar >= 'a' && buffChar <= 'z') || (buffChar >= 'A' && buffChar <= 'Z') || buffChar == '_') {
			sb.append((char)buffChar);
			nextChar();

			while (((buffChar >= 'a' && buffChar <= 'z') || (buffChar >= 'A' && buffChar <= 'Z') || buffChar == '_') || Character.isDigit(buffChar)) {
				sb.append((char)buffChar);
				nextChar();
			}

			String lexeme = sb.toString();
			// fun var if then else while do let in end
			Token.Symbol symbol;

			switch (lexeme) {
				case "fun":
					symbol = Token.Symbol.FUN;
					break;
				case "var":
					symbol = Token.Symbol.VAR;
					break;
				case "if":
					symbol = Token.Symbol.IF;
					break;
				case "then":
					symbol = Token.Symbol.THEN;
					break;
				case "else":
					symbol = Token.Symbol.ELSE;
					break;
				case "while":
					symbol = Token.Symbol.WHILE;
					break;
				case "do":
					symbol = Token.Symbol.DO;
					break;
				case "let":
					symbol = Token.Symbol.LET;
					break;
				case "in":
					symbol = Token.Symbol.IN;
					break;
				case "end":
					symbol = Token.Symbol.END;
					break;
				default:
					symbol = Token.Symbol.IDENTIFIER;
					break;
			}

			buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), symbol, sb.toString());
			return;

		} else {

			switch (buffChar) {
				case '=' :
					sb.append((char)buffChar);
					nextChar();
					if (buffChar == '=') {
						sb.append((char)buffChar);
						nextChar();
						buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.EQU, sb.toString());
					} else {
						buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.ASSIGN, sb.toString());
					}
					return;
				case ';' :
					sb.append((char)buffChar);
					nextChar();
					buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.SEMIC, sb.toString());
					return;
				case ',' :
					sb.append((char)buffChar);
					nextChar();
					buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.COMMA, sb.toString());
					return;
				case '&' :
					sb.append((char)buffChar);
					nextChar();
					if (buffChar == '&') {
						sb.append((char)buffChar);
						nextChar();
						buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.AND, sb.toString());
						return;
					} else {
						throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Nepričakovan znak: " + (char)buffChar);
					}
				case '|' :
					sb.append((char)buffChar);
					nextChar();
					if (buffChar == '|') {
						sb.append((char)buffChar);
						nextChar();
						buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.OR, sb.toString());
						return;
					} else {
						throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Nepričakovan znak: " + (char)buffChar);
					}
				case '!' :
					sb.append((char)buffChar);
					nextChar();
					if (buffChar == '=') {
						sb.append((char)buffChar);
						nextChar();
						buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.NEQ, sb.toString());
						return;
					}
					buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.NOT, sb.toString());
					return;
				case '<' :
					sb.append((char)buffChar);
					nextChar();
					if (buffChar == '=') {
						sb.append((char)buffChar);
						nextChar();
						buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.LEQ, sb.toString());
						return;
					}
					buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.LTH, sb.toString());
					return;
				case '>' :
					sb.append((char)buffChar);
					nextChar();
					if (buffChar == '=') {
						sb.append((char)buffChar);
						nextChar();
						buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.GEQ, sb.toString());
						return;
					}
					buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.GTH, sb.toString());
					return;
				case '+' :
					sb.append((char)buffChar);
					nextChar();
					buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.ADD, sb.toString());
					return;
				case '-' :
					sb.append((char)buffChar);
					nextChar();
					buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.SUB, sb.toString());
					return;
				case '*' :
					sb.append((char)buffChar);
					nextChar();
					buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.MUL, sb.toString());
					return;
				case '/' :
					sb.append((char)buffChar);
					nextChar();
					// komentarji
					if (buffChar == '/') {
						while (buffChar != '\n' && buffChar != -1) {
							nextChar();
						}
						nextToken();
						return;
					} else {
						buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.DIV, sb.toString());
						return;
					}
				case '%' :
					sb.append((char)buffChar);
					nextChar();
					buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.MOD, sb.toString());
					return;
				case '^' :
					sb.append((char)buffChar);
					nextChar();
					buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.PTR, sb.toString());
					return;
				case '(' :
					sb.append((char)buffChar);
					nextChar();
					buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.LPAREN, sb.toString());
					return;
				case ')' :
					sb.append((char)buffChar);
					nextChar();
					buffToken = new Token(new Report.Location(startCharLine, startCharCol, buffCharLine, buffCharColumn-1), Token.Symbol.RPAREN, sb.toString());
					return;
				default:
					throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Nepričakovan znak: " + (char)buffChar);
			}
		}
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki ostane v lastnistvu leksikalnega
	 * analizatorja.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token peekToken() {
		if (buffToken == null)
			nextToken();
		return buffToken;
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki preide v lastnistvo klicoce kode.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token takeToken() {
		if (buffToken == null)
			nextToken();
		final Token thisToken = buffToken;
		buffToken = null;
		return thisToken;
	}

	// --- ZAGON ---

	/**
	 * Zagon leksikalnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (lexical analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (LexAn lexAn = new LexAn(cmdLineArgs[0])) {
				while (lexAn.peekToken().symbol() != Token.Symbol.EOF)
					System.out.println(lexAn.takeToken());
				System.out.println(lexAn.takeToken());
			}

			// Upajmo, da kdaj pridemo to te tocke.
			// A zavedajmo se sledecega:
			// 1. Prevod je zaradi napak v programu lahko napacen :-o
			// 2. Izvorni program se zdalec ni tisto, kar je programer hotel, da bi bil ;-)
			Report.info("Done.");
		} catch (Report.Error error) {
			// Izpis opisa napake.
			System.err.println(error.getMessage());
			System.exit(1);
		}
	}

}
