package pins26.phase;

import pins26.common.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;
	private Token bufferedToken = null;
	private HashMap<AST.Node, Report.Locatable> attrLoc;

	/**
	 * Ustvari nov sintaksni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public SynAn(final String srcFileName) {
		this.lexAn = new LexAn(srcFileName);
	}

	@Override
	public void close() {
		lexAn.close();
	}

	/**
	 * Prevzame leksikalni analizator od leksikalnega analizatorja in preveri, ali
	 * je prave vrste.
	 * 
	 * @param symbol Pricakovana vrsta leksikalnega simbola.
	 * @return Prevzeti leksikalni simbol.
	 */

	// vzemi naslednji simbol in zahtevaj, da je pricakovane vrste (sicer napaka)
	private Token check(Token.Symbol symbol) {
		Token token = (bufferedToken != null) ? bufferedToken : lexAn.takeToken();
		bufferedToken = null;
		if (token.symbol() != symbol)
			throw new Report.Error(token, "Unexpected symbol '" + token.lexeme() + "'.");
		return token;
	}

	// poglej naslednji simbol, a ga ne porabi
	private Token peek() {
		if (bufferedToken != null) return bufferedToken;
		return lexAn.peekToken();
	}

	/*
	High level

    program -> definitions
    definitions -> definition definitions | epsilon
    definition -> fun IDENTIFIER ( parameters ) funBody | var IDENTIFIER = initializers
    funBody -> = statements | epsilon
    parameters -> IDENTIFIER paramTail | epsilon
    paramTail -> , IDENTIFIER paramTail | epsilon

	Statements

    statements -> statement ; statementTail
    statementTail -> statement ; statementTail | epsilon
    statement -> expression assiganPart | if expression then statements elsePart end |
    				while expression do statements end | let definitions in statements end
	assiganPart -> = expression | epsilon
    elsePart -> else statements | epsilon

	Initializers

    initializers -> initializer initTail | epsilon
    initTail -> , initializer initTail | epsilon
    initializer -> multiplicity const
    multiplicity -> INTCONST * | epsilon

	Expressions

    expression -> disjunction
    disjunction -> conjunction disjunctionTail
    disjunctionTail -> or conjunction disjunctionTail | epsilon
    conjunction -> comparison conjunctionTail
    conjunctionTail -> and comparison conjunctionTail | epsilon
    comparison -> additive comparisonTail
    comparisonTail -> relationalOp additive | epsilon
    additive -> multiplicative add_tail
    add_tail -> + multiplicative add_tail | - multiplicative additiveTail | epsilon
    multiplicative -> prefix multiTail
    multiTail -> * prefix multiTail | / prefix multiTail | % prefix multiTail | epsilon
    prefix -> ! prefix | + prefix | - prefix | ^ prefix | postfix
    postfix -> primary ptrTail
    ptrTail -> ^ ptrTail | epsilon

	Primary

    primary -> const | IDENTIFIER callArguments | (expression)
    callArguments -> ( arguments ) | epsilon
    arguments -> expression argumentsTail | epsilon
    argumentsTail -> , expression argumentsTail | epsilon
    const -> INTCONST | CHARCONST | STRINGCONST

    */

	/**
	 * Opravi sintaksno analizo.
	 */

	public AST.Node parse(HashMap<AST.Node, Report.Locatable> attrLoc) {
		this.attrLoc = attrLoc;
		final AST.Nodes<AST.MainDef> defs = parseProgram();
		if (lexAn.peekToken().symbol() != Token.Symbol.EOF)
			Report.warning(lexAn.peekToken(),
					"Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
		return defs;
	}

	public AST.Nodes<AST.MainDef> parseProgram() {

		AST.Nodes<AST.MainDef> defs = parseDefinitions();
		return defs;
	}

	private AST.Nodes<AST.MainDef> parseDefinitions() {

		List<AST.MainDef> defList = new ArrayList<>();
		Token startToken = peek();

		// beri definicije, dokler na vrsti vidis 'var' ali 'fun'
		while (peek().symbol() == Token.Symbol.VAR ||
				peek().symbol() == Token.Symbol.FUN) {

			defList.add(parseDefinition());
		}

		AST.Nodes<AST.MainDef> nodes = new AST.Nodes<>(defList);

		if (!defList.isEmpty()) {
			Report.Locatable start = attrLoc.get(defList.getFirst());
			Report.Locatable end = attrLoc.get(defList.getLast());
			attrLoc.put(nodes, new Report.Location(start, end));
		} else {
			attrLoc.put(nodes, startToken);
		}

		return nodes;
	}

	private AST.MainDef parseDefinition() {

		// 'fun' -> definicija funkcije, sicer definicija spremenljivke
		if (peek().symbol() == Token.Symbol.FUN) {
			return parseFunDef();

		} else {
			return parseVarDef();
		}
	}

	private AST.FunDef parseFunDef() {

		// fun IME ( parametri ) [= telo]
		Token funToken = check(Token.Symbol.FUN);
		Token nameToken = check(Token.Symbol.IDENTIFIER);
		check(Token.Symbol.LPAREN);

		AST.Nodes<AST.ParDef> params = parseParameters();

		check(Token.Symbol.RPAREN);

		AST.Nodes<AST.Stmt> body = parseFunBody();

		AST.FunDef funDef = new AST.FunDef(nameToken.lexeme(), params.getAll(), body.getAll());

		Report.Locatable end = attrLoc.get(body);
		attrLoc.put(funDef, new Report.Location(funToken, end));

		return funDef;
	}

	private AST.VarDef parseVarDef() {

		// var IME = inicializatorji
		Token varToken = check(Token.Symbol.VAR);
		Token nameToken = check(Token.Symbol.IDENTIFIER);
		check(Token.Symbol.ASSIGN);


		AST.Nodes<AST.Init> inits = parseInitializers();

		AST.VarDef varDef = new AST.VarDef(nameToken.lexeme(), inits.getAll());

		Report.Locatable initsLoc = attrLoc.get(inits);

		attrLoc.put(varDef.inits, initsLoc);

		attrLoc.put(varDef, new Report.Location(varToken, initsLoc));

		return varDef;
	}

	private AST.Nodes<AST.Stmt> parseFunBody() {

		List<AST.Stmt> stmts = new ArrayList<>();
		Token startToken = peek();


		// '=' pomeni telo s stavki; brez njega je prototip (prazno telo — npr. vgrajene funkcije)
		if (peek().symbol() == Token.Symbol.ASSIGN) {
			check(Token.Symbol.ASSIGN);

			AST.Nodes<AST.Stmt> bodyNodes = parseStatements();

			return bodyNodes;
		} else {
			AST.Nodes<AST.Stmt> emptyNodes = new AST.Nodes<>(stmts);
			attrLoc.put(emptyNodes, startToken);

			return emptyNodes;
		}
	}

	private AST.Nodes<AST.ParDef> parseParameters() {

		List<AST.ParDef> list = new ArrayList<>();
		Token startToken = peek();


		// nic ali vec imen parametrov, locenih z vejico
		if (peek().symbol() == Token.Symbol.IDENTIFIER) {
			list.add(parseParameter());

			while (peek().symbol() == Token.Symbol.COMMA) {
				check(Token.Symbol.COMMA);
				list.add(parseParameter());
			}
		}

		AST.Nodes<AST.ParDef> nodes = new AST.Nodes<>(list);

		if (!list.isEmpty()) {
			attrLoc.put(nodes, new Report.Location(attrLoc.get(list.getFirst()), attrLoc.get(list.getLast())));
		} else {
			attrLoc.put(nodes, startToken);
		}

		return nodes;
	}

	private AST.ParDef parseParameter() {

		Token nameToken = check(Token.Symbol.IDENTIFIER);

		AST.ParDef parDef = new AST.ParDef(nameToken.lexeme());
		attrLoc.put(parDef, nameToken);
		return parDef;
	}

	private AST.Nodes<AST.Stmt> parseStatements() {

		List<AST.Stmt> list = new ArrayList<>();
		Token startToken = peek();

		// stavki, vsak zakljucen s podpicjem
		while (isAStatement()) {
			AST.Stmt s = parseStatement();
			list.add(s);

			check(Token.Symbol.SEMIC);

		}

		AST.Nodes<AST.Stmt> nodes = new AST.Nodes<>(list);

		if (!list.isEmpty()) {
			Report.Locatable start = attrLoc.get(list.getFirst());
			Report.Locatable end = attrLoc.get(list.getLast());
			attrLoc.put(nodes, new Report.Location(start, end));
		} else {
			attrLoc.put(nodes, startToken);
		}

		return nodes;
	}

	private AST.Stmt parseStatement() {

		Token startToken = peek();

		// glede na prvi simbol izberi vrsto stavka: if / while / let / (izraz [= izraz])
		if (startToken.symbol() == Token.Symbol.IF) {
			// if pogoj then stavki [else stavki] end
			check(Token.Symbol.IF);
			AST.Expr condition = parseExpression();
			check(Token.Symbol.THEN);
			AST.Nodes<AST.Stmt> thenStatements = parseStatements();
			AST.Nodes<AST.Stmt> elseStatements = parseElsePart();
			Token endToken = check(Token.Symbol.END);

			AST.IfStmt ifstmt = new AST.IfStmt(condition, thenStatements.getAll(), elseStatements.getAll());
			attrLoc.put(ifstmt, new Report.Location(startToken, endToken));

			return ifstmt;

		} else if (startToken.symbol() == Token.Symbol.WHILE) {
			// while pogoj do stavki end
			check(Token.Symbol.WHILE);
			AST.Expr condition = parseExpression();
			check(Token.Symbol.DO);
			AST.Nodes<AST.Stmt> whileStatements = parseStatements();
			Token endToken = check(Token.Symbol.END);

			AST.WhileStmt whileStmt = new AST.WhileStmt(condition, whileStatements.getAll());
			attrLoc.put(whileStmt, new Report.Location(startToken, endToken));

			return whileStmt;

		} else if (startToken.symbol() == Token.Symbol.LET) {
			// let definicije in stavki end
			check(Token.Symbol.LET);
			AST.Nodes<AST.MainDef> defs = parseDefinitions();
			check(Token.Symbol.IN);
			AST.Nodes<AST.Stmt> body = parseStatements();
			Token endToken = check(Token.Symbol.END);

			List<AST.MainDef> mainDefs = (List<AST.MainDef>)(List<?>) defs.getAll();

			AST.LetStmt letStmt = new AST.LetStmt(mainDefs, body.getAll());

			attrLoc.put(letStmt, new Report.Location(startToken, endToken));
			return letStmt;

		} else {
			// sicer: izraz, ki mu lahko sledi '= izraz' (prireditev)
			AST.Expr expr = parseExpression();
			return parseAssignPart(expr);
		}
	}

	private AST.Nodes<AST.Stmt> parseElsePart() {
		if (peek().symbol() == Token.Symbol.ELSE) {
			check(Token.Symbol.ELSE);
			return parseStatements();
		} else {
			AST.Nodes<AST.Stmt> empty = new AST.Nodes<>(new ArrayList<>());
			attrLoc.put(empty, peek());
			return empty;
		}
	}

	private AST.Stmt parseAssignPart(AST.Expr leftExpr) {
		// '=' -> prireditveni stavek (leva = desna); sicer samo izrazni stavek
		if (peek().symbol() == Token.Symbol.ASSIGN) {

			Token assignToken = take();
			AST.Expr rightExpr = parseExpression();

			AST.AssignStmt assign = new AST.AssignStmt(leftExpr, rightExpr);

			Report.Locatable start = attrLoc.get(leftExpr);
			Report.Locatable end = attrLoc.get(rightExpr);
			attrLoc.put(assign, new Report.Location(start, end));

			return assign;
		} else {

			AST.ExprStmt exprStmt = new AST.ExprStmt(leftExpr);
			attrLoc.put(exprStmt, attrLoc.get(leftExpr));
			return exprStmt;
		}
	}

	private AST.Nodes<AST.Init> parseInitializers() {

		List<AST.Init> initList = new ArrayList<>();
		Token startToken = peek();

		initList.add(parseInitializer());

		while (peek().symbol() == Token.Symbol.COMMA) {
			check(Token.Symbol.COMMA);
			initList.add(parseInitializer());
		}

		AST.Nodes<AST.Init> nodes = new AST.Nodes<>(initList);

		Report.Locatable start = attrLoc.get(initList.getFirst());
		Report.Locatable end = attrLoc.get(initList.getLast());

		attrLoc.put(nodes, new Report.Location(start, end));

		return nodes;
	}

	private AST.Init parseInitializer() {
		Token startToken = peek();

		// privzeta ponovitev je 1
		AST.AtomExpr countExpr = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1");
		attrLoc.put(countExpr, startToken);

		// oblika n*k: ce je prvi simbol INTCONST in mu sledi '*', je to stevilo ponovitev
		if (peek().symbol() == Token.Symbol.INTCONST) {
			Token numToken = take();

			if (peek().symbol() == Token.Symbol.MUL) {
				check(Token.Symbol.MUL);

				countExpr = new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, numToken.lexeme());
				attrLoc.put(countExpr, numToken);
			} else {
				// ni bilo '*': INTCONST je v resnici vrednost -> vrni ga nazaj v medpomnilnik
				this.bufferedToken = numToken;
			}
		}

		// vrednost k
		AST.AtomExpr valueExpr = parseConst();

		AST.Init initNode = new AST.Init(countExpr, valueExpr);

		attrLoc.put(initNode, new Report.Location(startToken, attrLoc.get(valueExpr)));

		return initNode;
	}

	private void parseMultiplicity() {
		if (peek().symbol() == Token.Symbol.INTCONST) {
			Token t = lexAn.takeToken();
			if (lexAn.peekToken().symbol() == Token.Symbol.MUL) {
				System.out.println("multiplicity -> INTCONST *");
				check(Token.Symbol.MUL);
			} else {
				System.out.println("multiplicity -> epsilon");
				this.bufferedToken = t;
			}
		} else {
			System.out.println("multiplicity -> epsilon");
		}
	}

	// ---------------------------------------------------------------------------------------------------------

	// precedenčna lestev operatorjev

	private AST.Expr parseExpression() {
		return parseDisjunction();
	}

	private AST.Expr parseDisjunction() {

		// || (disjunkcija), najnizja prioriteta, levo asociativna (zanka kopici levi operand)
		AST.Expr left = parseConjunction();

		while (peek().symbol() == Token.Symbol.OR) {
			Token opToken = take();
			AST.Expr right = parseConjunction();

			AST.BinExpr binExpr = new AST.BinExpr(AST.BinExpr.Oper.OR, left, right);
			attrLoc.put(binExpr, new Report.Location(attrLoc.get(left), attrLoc.get(right)));

			left = binExpr;
		}
		return left;
	}

	private AST.Expr parseConjunction() {

		// && (konjunkcija), levo asociativna
		AST.Expr left = parseComparison();

		while (peek().symbol() == Token.Symbol.AND) {
			Token opToken = take();
			AST.Expr right = parseComparison();

			AST.BinExpr binExpr = new AST.BinExpr(AST.BinExpr.Oper.AND, left, right);
			attrLoc.put(binExpr, new Report.Location(attrLoc.get(left), attrLoc.get(right)));

			left = binExpr;
		}
		return left;
	}

	private AST.Expr parseComparison() {

		// primerjave == != < > <= >= : NISO asociativne -> kvecjemu ena (if, ne zanka)
		AST.Expr left = parseAdditive();

		Token.Symbol s = peek().symbol();
		if (s == Token.Symbol.EQU || s == Token.Symbol.NEQ || s == Token.Symbol.LTH ||
				s == Token.Symbol.GTH || s == Token.Symbol.LEQ || s == Token.Symbol.GEQ) {

			Token opToken = take();
			AST.Expr right = parseAdditive();

			AST.BinExpr.Oper oper = switch (opToken.symbol()) {
				case EQU -> AST.BinExpr.Oper.EQU;
				case NEQ -> AST.BinExpr.Oper.NEQ;
				case LTH -> AST.BinExpr.Oper.LTH;
				case GTH -> AST.BinExpr.Oper.GTH;
				case LEQ -> AST.BinExpr.Oper.LEQ;
				case GEQ -> AST.BinExpr.Oper.GEQ;
				default -> throw new Report.InternalError();
			};

			AST.BinExpr binExpr = new AST.BinExpr(oper, left, right);
			attrLoc.put(binExpr, new Report.Location(attrLoc.get(left), attrLoc.get(right)));
			return binExpr;
		}
		return left;
	}

	private AST.Expr parseAdditive() {

		// sestevanje/odstevanje + - , levo asociativno (desni operand klice nivo nizje)
		AST.Expr left = parseMultiplicative();

		while (peek().symbol() == Token.Symbol.ADD || peek().symbol() == Token.Symbol.SUB) {

			Token operatorToken = take();

			AST.Expr right = parseMultiplicative();

			AST.BinExpr.Oper oper = (operatorToken.symbol() == Token.Symbol.ADD) ? AST.BinExpr.Oper.ADD : AST.BinExpr.Oper.SUB;
			AST.BinExpr binExpr = new AST.BinExpr(oper, left, right);

			Report.Locatable start = attrLoc.get(left);
			Report.Locatable end = attrLoc.get(right);
			attrLoc.put(binExpr, new Report.Location(start, end));


			left = binExpr;
		}

		return left;
	}

	private AST.Expr parseMultiplicative() {

		// mnozenje/deljenje/ostanek * / % , levo asociativno, visja prioriteta od + -
		AST.Expr left = parsePrefix();

		while (peek().symbol() == Token.Symbol.MUL || peek().symbol() == Token.Symbol.DIV || peek().symbol() == Token.Symbol.MOD) {

			Token opToken = take();
			AST.Expr right = parsePrefix();

			AST.BinExpr.Oper oper = switch (opToken.symbol()) {
				case MUL -> AST.BinExpr.Oper.MUL;
				case DIV -> AST.BinExpr.Oper.DIV;
				case MOD -> AST.BinExpr.Oper.MOD;
				default -> throw new Report.InternalError();
			};

			AST.BinExpr binExpr = new AST.BinExpr(oper, left, right);
			attrLoc.put(binExpr, new Report.Location(attrLoc.get(left), attrLoc.get(right)));

			left = binExpr;
		}
		return left;
	}

	private AST.Expr parsePrefix() {

		// predponski operatorji ! + - ^ , desno asociativni (rekurzija na isti nivo)
		Token.Symbol sym = peek().symbol();

		if (isItAPrefix()) {

			Token opToken = take();

			AST.Expr child = parsePrefix();


			AST.UnExpr.Oper oper = switch (sym) {
				case NOT -> AST.UnExpr.Oper.NOT;
				case ADD -> AST.UnExpr.Oper.ADD;
				case SUB -> AST.UnExpr.Oper.SUB;
				case PTR -> AST.UnExpr.Oper.MEMADDR;
				default  -> throw new Report.InternalError();
			};


			AST.UnExpr unExpr = new AST.UnExpr(oper, child);

			Report.Locatable childEnd = attrLoc.get(child);
			attrLoc.put(unExpr, new Report.Location(opToken, childEnd));

			return unExpr;
		}
		return parsePostfix();
	}

	private AST.Expr parsePostfix() {

		// zaponski '^' (dereferenca/vrednost na naslovu), najvisja prioriteta
		AST.Expr expr = parsePrimary();

		while (peek().symbol() == Token.Symbol.PTR) {

			Token ptrToken = take();

			AST.UnExpr dereference = new AST.UnExpr(AST.UnExpr.Oper.VALUEAT, expr);

			Report.Locatable startLoc = this.attrLoc.get(expr);
			this.attrLoc.put(dereference, new Report.Location(startLoc, ptrToken));

			expr = dereference;
		}

		return expr;
	}

	private Token take() {
		// vzemi vrnjeni (medpomnjeni) simbol, ce obstaja; sicer naslednjega iz leksikalnika
		Token t = (bufferedToken != null) ? bufferedToken : lexAn.takeToken();
		bufferedToken = null;
		return t;
	}

	private AST.AtomExpr parseConst() {

		// konstanta INTCONST / CHARCONST / STRINGCONST -> AtomExpr (znaku/nizu vrne narekovaje)
		Token t = take();

		AST.AtomExpr.Type type;

		String lexeme = t.lexeme();
		switch(t.symbol()) {
			case INTCONST:
				type = AST.AtomExpr.Type.INTCONST;
				break;
			case CHARCONST:
				type = AST.AtomExpr.Type.CHRCONST;
				lexeme = "'" + lexeme + "'";
				break;
			case STRINGCONST:
				type = AST.AtomExpr.Type.STRCONST;
				lexeme = "\"" + lexeme + "\"";
				break;
			default:
				throw new Report.Error(t, "Expected a constant.");
		}

		AST.AtomExpr atom = new AST.AtomExpr(type, lexeme);

		this.attrLoc.put(atom, t);
		return atom;
	}

	private AST.Expr parsePrimary() {
		// osnovni izraz: konstanta | ime (klic ali spremenljivka) | ( izraz )
		Token t = peek();

		// konstanta
		if (t.symbol() == Token.Symbol.INTCONST || t.symbol() == Token.Symbol.CHARCONST || t.symbol() == Token.Symbol.STRINGCONST) {

			return parseConst();
		}

		// ime: lahko klic funkcije ali uporaba spremenljivke (odloci parseCallArguments)
		if (t.symbol() == Token.Symbol.IDENTIFIER) {

			Token name = check(Token.Symbol.IDENTIFIER);
			return parseCallArguments(name);
		}

		// oklepaj: ( izraz )
		if (t.symbol() == Token.Symbol.LPAREN) {

			check(Token.Symbol.LPAREN);
			AST.Expr e = parseExpression();
			check(Token.Symbol.RPAREN);

			return e;
		}

		throw new Report.Error(t, "Expected an expression (constant, identifier, or parenthesis), but found '" + t.lexeme() + "'.");

	}

	private AST.Expr parseCallArguments(Token nameToken) {

		// brez oklepaja za imenom -> uporaba spremenljivke; z oklepajem -> klic funkcije
		if (peek().symbol() != Token.Symbol.LPAREN) {
			AST.VarExpr var = new AST.VarExpr(nameToken.lexeme());

			this.attrLoc.put(var, nameToken);
			return var;
		}

		check(Token.Symbol.LPAREN);
		AST.Nodes<AST.Expr> arguments = parseArguments();
		Token rparen = check(Token.Symbol.RPAREN);

		AST.CallExpr call = new AST.CallExpr(nameToken.lexeme(), arguments.getAll());

		Report.Location callLoc = new Report.Location(nameToken, rparen);
		this.attrLoc.put(call, callLoc);

		return call;
	}

	private AST.Nodes<AST.Expr> parseArguments() {

		List<AST.Expr> argList = new ArrayList<>();

		Token startToken = peek();

		if (isAnExpression()) {
			AST.Expr arg = parseExpression();
			argList.add(arg);

			while (peek().symbol() == Token.Symbol.COMMA) {
				check(Token.Symbol.COMMA);
				AST.Expr nxtArg = parseExpression();
				argList.add(nxtArg);
			}
		}

		AST.Nodes<AST.Expr> nodes = new AST.Nodes<>(argList);

		if (argList.isEmpty()) {
			this.attrLoc.put(nodes, startToken);
		} else {
			AST.Expr first = argList.getFirst();
			AST.Expr last = argList.getLast();

			Report.Locatable start = this.attrLoc.get(first);
			Report.Locatable end = this.attrLoc.get(last);

			this.attrLoc.put(nodes, new Report.Location(start, end));

		}
		return nodes;
	}


	// POMOŽNI — mnozice prvih simbolov (s cim se stavek/izraz lahko zacne)

	// se lahko tukaj zacne stavek? (if/while/let ali izraz)
	private boolean isAStatement() {
		Token.Symbol s = peek().symbol();
		return s == Token.Symbol.IF || s == Token.Symbol.WHILE || s == Token.Symbol.LET || isAnExpression();
	}

	// se lahko tukaj zacne izraz? (konstanta, ime, oklepaj ali predponski operator)
	private boolean isAnExpression() {
		Token.Symbol s = peek().symbol();
		return s == Token.Symbol.INTCONST || s == Token.Symbol.CHARCONST ||
				s == Token.Symbol.STRINGCONST || s == Token.Symbol.IDENTIFIER ||
				s == Token.Symbol.LPAREN || isItAPrefix();
	}

	// je trenutni simbol predponski operator?
	private boolean isItAPrefix() {
		Token.Symbol s = peek().symbol();
		return s == Token.Symbol.NOT || s == Token.Symbol.ADD || s == Token.Symbol.SUB || s == Token.Symbol.PTR;
	}

	// --- ZAGON ---

	/**
	 * Zagon sintaksnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (syntax analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				HashMap<AST.Node, Report.Locatable> attrLoc = new HashMap<>();

				//synAn.parse(attrLoc);

				AST.Node astRoot = synAn.parse(attrLoc);

				AST.AttrAST attrAST = new AST.AttrAST(astRoot);
				AST.Logger logger = new AST.Logger(attrAST);

				logger.log();
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
