package pins26.phase;

import java.util.*;

import pins26.common.*;

/**
 * Generiranje kode.
 */
public class CodeGen {

	@SuppressWarnings({ "doclint:missing" })
	public CodeGen() {
		throw new Report.InternalError();
	}

	/**
	 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 * predstavitve.
	 * 
	 * Atributi:
	 * <ol>
	 * <li>({@link Abstr}) lokacija kode, ki pripada posameznemu vozliscu;</li>
	 * <li>({@link SemAn}) definicija uporabljenega imena;</li>
	 * <li>({@link SemAn}) ali je dani izraz levi izraz;</li>
	 * <li>({@link Memory}) klicni zapis funkcije;</li>
	 * <li>({@link Memory}) dostop do parametra;</li>
	 * <li>({@link Memory}) dostop do spremenljivke;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo kodo programa;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo podatke programa.</li>
	 * </ol>
	 */
	public static class AttrAST extends Memory.AttrAST {

		/** Atribut: seznam ukazov, ki predstavljajo kodo programa. */
		public final Map<AST.Node, List<PDM.CodeInstr>> attrCode;

		/** Atribut: seznam ukazov, ki predstavljajo podatke programa. */
		public final Map<AST.Node, List<PDM.DataInstr>> attrData;

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST  Abstraktno sintaksno drevo z dodanimi atributi pomnilniske
		 *                 predstavitve.
		 * @param attrCode Attribut: seznam ukazov, ki predstavljajo kodo programa.
		 * @param attrData Attribut: seznam ukazov, ki predstavljajo podatke programa.
		 */
		public AttrAST(final Memory.AttrAST attrAST, final Map<AST.Node, List<PDM.CodeInstr>> attrCode,
				final Map<AST.Node, List<PDM.DataInstr>> attrData) {
			super(attrAST);
			this.attrCode = attrCode;
			this.attrData = attrData;
		}

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi generiranja
		 *                kode.
		 */
		public AttrAST(final AttrAST attrAST) {
			super(attrAST);
			this.attrCode = attrAST.attrCode;
			this.attrData = attrAST.attrData;
		}

		@Override
		public String head(final AST.Node node, final boolean highlighted) {
			final StringBuffer head = new StringBuffer();
			head.append(super.head(node, false));
			return head.toString();
		}

		@Override
		public void desc(final int indent, final AST.Node node, final boolean highlighted) {
			super.desc(indent, node, false);
			System.out.print(highlighted ? "\033[31m" : "");
			if (attrCode.get(node) != null) {
				List<PDM.CodeInstr> instrs = attrCode.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Code: ---\n");
					for (final PDM.CodeInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			if (attrData.get(node) != null) {
				List<PDM.DataInstr> instrs = attrData.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Data: ---\n");
					for (final PDM.DataInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			System.out.print(highlighted ? "\033[30m" : "");
			return;
		}

	}

	/**
	 * Izracuna kodo programa
	 * 
	 * @param memoryAttrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
	 *                      pomnilniske predstavitve.
	 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 *         predstavitve.
	 */
	public static AttrAST generate(final Memory.AttrAST memoryAttrAST) {
		AttrAST attrAST = new AttrAST(memoryAttrAST, new HashMap<AST.Node, List<PDM.CodeInstr>>(),
				new HashMap<AST.Node, List<PDM.DataInstr>>());
		(new CodeGenerator(attrAST)).generate();
		return attrAST;
	}

	/**
	 * Generiranje kode v abstraktnem sintaksnem drevesu.
	 */
	private static class CodeGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Stevec anonimnih label. */
		private int labelCounter = 0;

		/**
		 * Ustvari nov generator kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Sprozi generiranje kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 *         predstavitve.
		 */
		public AttrAST generate() {
			attrAST.ast.accept(new Generator(), null);
			return new AttrAST(attrAST, Collections.unmodifiableMap(attrAST.attrCode),
					Collections.unmodifiableMap(attrAST.attrData));
		}

		/** Obiskovalec, ki generira kodo v abstraktnem sintaksnem drevesu. */
		private class Generator implements AST.FullVisitor<List<PDM.CodeInstr>, Mem.Frame> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			/* TODO */

			@Override
			public List<PDM.CodeInstr> visit(final AST.Nodes<? extends AST.Node> nodes, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();
				// koda vsakega otroka, zlepljena po vrsti v en seznam
				for (final AST.Node node : nodes) {
					List<PDM.CodeInstr> nodeCode = node.accept(this, frame);
					if (nodeCode != null)
						code.addAll(nodeCode);
				}
				return code;
			}


			@Override
			public List<PDM.CodeInstr> visit(final AST.AtomExpr atomExpr, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();

				if (atomExpr.type == AST.AtomExpr.Type.INTCONST) {
					// celo stevilo: vrednost na sklad
					code.add(new PDM.PUSH(Integer.parseInt(atomExpr.value), attrAST.attrLoc.get(atomExpr)));
				} else if (atomExpr.type == AST.AtomExpr.Type.CHRCONST) {
					// znak: koda znaka na sklad
					code.add(new PDM.PUSH((int) atomExpr.value.charAt(1), attrAST.attrLoc.get(atomExpr)));
				} else if (atomExpr.type == AST.AtomExpr.Type.STRCONST) {
					// niz: na sklad naslov, znaki gredo v podatkovni segment
					String label = "str_" + labelCounter++;
					code.add(new PDM.NAME(label, attrAST.attrLoc.get(atomExpr)));

					List<PDM.DataInstr> data = new ArrayList<>();
					data.add(new PDM.LABEL(label, attrAST.attrLoc.get(atomExpr)));

					// vsak znak kot en podatek
					for (int i = 1; i < atomExpr.value.length()-1; i++) {
						char ch = atomExpr.value.charAt(i);
						data.add(new PDM.DATA((int) ch, attrAST.attrLoc.get(atomExpr)));
					}
					// 0 na koncu = zakljucek niza
					data.add(new PDM.DATA(0, attrAST.attrLoc.get(atomExpr)));
					attrAST.attrData.put(atomExpr, data);
				}
				attrAST.attrCode.put(atomExpr, code);
				return code;
			}

			public List<PDM.CodeInstr> visit(final AST.BinExpr binExpr, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();

				// izracunaj levi in desni operand -> oba sta na skladu
				code.addAll(binExpr.fstExpr.accept(this, frame));
				code.addAll(binExpr.sndExpr.accept(this, frame));

				// pretvori operator iz AST v operator stroja
				PDM.OPER.Oper oper = switch (binExpr.oper) {
                    case ADD -> PDM.OPER.Oper.ADD;
                    case SUB -> PDM.OPER.Oper.SUB;
                    case MUL -> PDM.OPER.Oper.MUL;
                    case DIV -> PDM.OPER.Oper.DIV;
                    case MOD -> PDM.OPER.Oper.MOD;
                    case EQU -> PDM.OPER.Oper.EQU;
                    case NEQ -> PDM.OPER.Oper.NEQ;
                    case GTH -> PDM.OPER.Oper.GTH;
                    case LTH -> PDM.OPER.Oper.LTH;
                    case GEQ -> PDM.OPER.Oper.GEQ;
                    case LEQ -> PDM.OPER.Oper.LEQ;
                    case AND -> PDM.OPER.Oper.AND;
                    case OR -> PDM.OPER.Oper.OR;
                };

                // operator pobere oba operanda s sklada in pusti rezultat
                code.add(new PDM.OPER(oper, attrAST.attrLoc.get(binExpr)));
				attrAST.attrCode.put(binExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.UnExpr unExpr, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();

				// izracunaj operand -> njegova vrednost je na skladu
				code.addAll(unExpr.expr.accept(this, frame));

				switch (unExpr.oper) {
					case NOT: code.add(new PDM.OPER(PDM.OPER.Oper.NOT, attrAST.attrLoc.get(unExpr))); break;
					case SUB: code.add(new PDM.OPER(PDM.OPER.Oper.NEG, attrAST.attrLoc.get(unExpr))); break; // negacija
					case ADD: break; // enojni plus: ne naredi nic
					case MEMADDR: code.removeLast(); break; // naslov namesto vrednosti: odstrani zadnji LOAD
					case VALUEAT: code.add(new PDM.LOAD(attrAST.attrLoc.get(unExpr))); break; // dereferenca: beri z naslova
					default: throw new Report.InternalError();
				}

				attrAST.attrCode.put(unExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.ExprStmt exprStmt, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();

				// izracunaj izraz (pusti vrednost na skladu)
				code.addAll(exprStmt.expr.accept(this, frame));
				// zavrzi vrednost (stavek ne sme pustiti nicesar na skladu)
				code.add(new PDM.PUSH(4, attrAST.attrLoc.get(exprStmt)));
				code.add(new PDM.POPN(attrAST.attrLoc.get(exprStmt)));

				attrAST.attrCode.put(exprStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.AssignStmt assignStmt, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();
				// vrednost desne strani na sklad
				code.addAll(assignStmt.srcExpr.accept(this, frame));
				// koda leve strani brez zadnjega LOAD -> dobimo NASLOV, ne vrednosti
				List<PDM.CodeInstr> dstCode = new ArrayList<>(assignStmt.dstExpr.accept(this, frame));
				dstCode.removeLast();
				code.addAll(dstCode);
				// shrani vrednost na naslov
				code.add(new PDM.SAVE(attrAST.attrLoc.get(assignStmt)));
				attrAST.attrCode.put(assignStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.VarExpr varExpr, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(varExpr);

				// poisci dostop do imena: spremenljivka ali parameter
				Mem.Access access = switch (attrAST.attrDef.get(varExpr)) {
					case AST.VarDef varDef -> attrAST.attrVarAccess.get(varDef);
					case AST.ParDef parDef -> attrAST.attrParAccess.get(parDef);
					default -> throw new Report.InternalError();
				};

				// najprej na sklad daj NASLOV spremenljivke
				switch (access) {
					case Mem.AbsAccess absAccess -> {
						// globalna: naslov je kar oznaka
						code.add(new PDM.NAME(absAccess.name, loc));
					}
					case Mem.RelAccess relAccess -> {
						// lokalna/parameter: FP + skoki po staticni povezavi + odmik
						code.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));
						// po en LOAD za vsak nivo navzven do okvira, kjer spremenljivka zivi
						for (int i = 0; i < frame.depth - relAccess.depth; i++)
							code.add(new PDM.LOAD(loc));
						code.add(new PDM.PUSH(relAccess.offset, loc));
						code.add(new PDM.OPER(PDM.OPER.Oper.ADD, loc));
					}
					default -> throw new Report.InternalError();
				}

				// preberi vrednost z naslova
				code.add(new PDM.LOAD(loc));

				attrAST.attrCode.put(varExpr, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.FunDef funDef, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();
				Mem.Frame myFrame = attrAST.attrFrame.get(funDef);
				Report.Locatable loc = attrAST.attrLoc.get(funDef);

				// vstopna oznaka funkcije
				code.add(new PDM.LABEL(myFrame.name, loc));
				// prolog: naredi prostor za lokalne spremenljivke (negativni POPN doda nicle)
				code.add(new PDM.PUSH(8 - myFrame.varsSize, loc));
				code.add(new PDM.POPN(loc));

				// telo funkcije (generirano v njenem lastnem klicnem zapisu myFrame)
				code.addAll(funDef.stmts.accept(this, myFrame));
				// odstrani PUSH 4 in POPN zadnjega stavka, da njegova vrednost ostane na skladu = rezultat funkcije
				if (funDef.stmts.size() > 0) {
					code.removeLast();
					code.removeLast();
				}

				// epilog: vrnitev iz funkcije (RETN pocisti klicni zapis)
				code.add(new PDM.PUSH(myFrame.parsSize - 4, loc));
				code.add(new PDM.RETN(myFrame, loc));

				attrAST.attrCode.put(funDef, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.VarDef varDef, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();
				List<PDM.DataInstr> data = new ArrayList<>();

				Report.Locatable loc = attrAST.attrLoc.get(varDef);
				String label = "init_" + labelCounter++;
				Mem.Access access = attrAST.attrVarAccess.get(varDef);

				// globalna spremenljivka: ima stalen prostor v podatkovnem segmentu
				if (access instanceof Mem.AbsAccess absAccess) {
					// predloga zacetnih vrednosti
					data.add(new PDM.LABEL(label, loc));
					for (Integer val : absAccess.inits)
						data.add(new PDM.DATA(val, loc));

					// prostor same spremenljivke
					data.add(new PDM.LABEL(absAccess.name, loc));
					data.add(new PDM.SIZE(absAccess.size, loc));
					attrAST.attrData.put(varDef, data);

					// ob zagonu: prepisi predlogo v spremenljivko (naslov je oznaka)
					code.add(new PDM.NAME(absAccess.name, loc));
					code.add(new PDM.NAME(label, loc));
					code.add(new PDM.INIT(loc));

				} else if (access instanceof Mem.RelAccess relAccess) {
					// lokalna spremenljivka: zivi na skladu
					// predloga zacetnih vrednosti (gre v podatke)
					data.add(new PDM.LABEL(label, loc));
					for (Integer val : relAccess.inits)
						data.add(new PDM.DATA(val, loc));

					attrAST.attrData.put(varDef, data);

					// izracunaj naslov na skladu (FP + odmik) in prepisi predlogo vanj
					code.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));
					code.add(new PDM.PUSH(relAccess.offset, loc));
					code.add(new PDM.OPER(PDM.OPER.Oper.ADD, loc));
					code.add(new PDM.NAME(label, loc));
					code.add(new PDM.INIT(loc));
				}

				attrAST.attrCode.put(varDef, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.LetStmt letStmt, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();

				// definicije: pri spremenljivkah vkljuci kodo za inicializacijo,
				// funkcije samo obisci (njihova koda gre v poseben segment)
				for (AST.MainDef def : letStmt.defs) {
					if (def instanceof AST.VarDef)
						code.addAll(def.accept(this, frame));
					else
						def.accept(this, frame);
				}
				// nato koda stavkov v telesu (in ... end)
				code.addAll(letStmt.stmts.accept(this, frame));
				attrAST.attrCode.put(letStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.IfStmt ifStmt, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(ifStmt);

				String thenLabel = "if_" + labelCounter++;
				String elseLabel = "else_" + labelCounter++;
				String endLabel  = "end_"  + labelCounter++;

				// pogoj; pogojni skok: ce velja -> then, sicer -> else
				code.addAll(ifStmt.cond.accept(this, frame));
				code.add(new PDM.NAME(thenLabel, loc));
				code.add(new PDM.NAME(elseLabel, loc));
				code.add(new PDM.CJMP(loc));

				// veja then, nato brezpogojni skok cez else na konec
				code.add(new PDM.LABEL(thenLabel, loc));
				code.addAll(ifStmt.thenStmts.accept(this, frame));
				code.add(new PDM.NAME(endLabel, loc));
				code.add(new PDM.UJMP(loc));

				// veja else
				code.add(new PDM.LABEL(elseLabel, loc));
				code.addAll(ifStmt.elseStmts.accept(this, frame));

				// konec
				code.add(new PDM.LABEL(endLabel, loc));
				attrAST.attrCode.put(ifStmt, code);
				return code;
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.WhileStmt whileStmt, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();
				Report.Locatable loc = attrAST.attrLoc.get(whileStmt);

				String whileLabel = "while_" + labelCounter++;
				String bodyLabel  = "body_"  + labelCounter++;
				String endLabel   = "end_"   + labelCounter++;

				// zacetek zanke (sem se vracamo)
				code.add(new PDM.LABEL(whileLabel, loc));

				// pogoj; ce velja -> telo, sicer -> konec
				code.addAll(whileStmt.cond.accept(this, frame));

				code.add(new PDM.NAME(bodyLabel, loc));
				code.add(new PDM.NAME(endLabel, loc));
				code.add(new PDM.CJMP(loc));

				// telo, nato skok nazaj na zacetek (preverjanje pogoja)
				code.add(new PDM.LABEL(bodyLabel, loc));
				code.addAll(whileStmt.stmts.accept(this, frame));
				code.add(new PDM.NAME(whileLabel, loc));
				code.add(new PDM.UJMP(loc));

				// konec zanke
				code.add(new PDM.LABEL(endLabel, loc));

				attrAST.attrCode.put(whileStmt, code);
				return code;
			}


			@Override
			public List<PDM.CodeInstr> visit(final AST.CallExpr callExpr, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();

				Report.Locatable loc = attrAST.attrLoc.get(callExpr);

				// katero funkcijo klicemo in njen klicni zapis
				AST.FunDef callee = (AST.FunDef) attrAST.attrDef.get(callExpr);
				Mem.Frame calleeFrame = attrAST.attrFrame.get(callee);

				// argumenti na sklad od desne proti levi (prvi argument konca na vrhu)
				List<AST.Expr> args = callExpr.args.getAll();
				for (int i = args.size() - 1; i >= 0; i--)
					code.addAll(args.get(i).accept(this, frame));

				// staticna povezava: zacni pri svojem FP in skoci navzven po verigi
				int hops = frame.depth - calleeFrame.depth + 1;
				code.add(new PDM.REGN(PDM.REGN.Reg.FP, loc));
				for (int i = 0; i < hops; i++)
					code.add(new PDM.LOAD(loc));

				// naslov funkcije in klic
				code.add(new PDM.NAME(calleeFrame.name, loc));
				code.add(new PDM.CALL(calleeFrame, loc));

				attrAST.attrCode.put(callExpr, code);
				return code;
			}

		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo kodo programa.
	 */
	public static class CodeSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov za inicializacijo staticnih spremenljivk. */
		private final Vector<PDM.CodeInstr> codeInitSegment = new Vector<PDM.CodeInstr>();

		/** Seznam ukazov funkcij. */
		private final Vector<PDM.CodeInstr> codeFunsSegment = new Vector<PDM.CodeInstr>();

		/** Klicni zapis funkcije {@code main}. */
		private Mem.Frame main = null;

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo kodo programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo kodo programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo kodo programa.
		 */
		public List<PDM.CodeInstr> codeSegment() {
			// zberi kodo funkcij in inicializacij globalnih spremenljivk
			attrAST.ast.accept(new Generator(), null);
			// zagonska koda: poklici main (pred njim staticna povezava 0)
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("main", null));
			codeInitSegment.addLast(new PDM.CALL(main, null));
			// po koncu main: poklici exit
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("exit", null));
			codeInitSegment.addLast(new PDM.CALL(null, null));
			// najprej init segment, nato segment funkcij
			final Vector<PDM.CodeInstr> codeSegment = new Vector<PDM.CodeInstr>();
			codeSegment.addAll(codeInitSegment);
			codeSegment.addAll(codeFunsSegment);
			return Collections.unmodifiableList(codeSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo kodo programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.FunDef funDef, final Object arg) {
				// prototipi (brez telesa) so vgrajene funkcije -> preskoci
				if (funDef.stmts.size() == 0)
					return null;
				// dodaj kodo funkcije v segment funkcij
				List<PDM.CodeInstr> code = attrAST.attrCode.get(funDef);
				codeFunsSegment.addAll(code);
				funDef.pars.accept(this, arg);
				funDef.stmts.accept(this, arg);
				// zapomni si klicni zapis funkcije main (potreben za zagonsko kodo)
				switch (funDef.name) {
				case "main" -> main = attrAST.attrFrame.get(funDef);
				}
				return null;
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				switch (attrAST.attrVarAccess.get(varDef)) {
				// globalna: koda za inicializacijo gre v init segment
				case Mem.AbsAccess __: {
					List<PDM.CodeInstr> code = attrAST.attrCode.get(varDef);
					codeInitSegment.addAll(code);
					break;
				}
				// lokalna: inicializira se v telesu funkcije, tukaj nic
				case Mem.RelAccess __: {
					break;
				}
				default:
					throw new Report.InternalError();
				}
				return null;
			}



		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo podatke programa.
	 */
	public static class DataSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov, ki predstavljajo podatke programa. */
		private final Vector<PDM.DataInstr> dataSegment = new Vector<PDM.DataInstr>();

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo podatke programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public DataSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo podatke programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo podatke programa.
		 */
		public List<PDM.DataInstr> dataSegment() {
			attrAST.ast.accept(new Generator(), null);
			return Collections.unmodifiableList(dataSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo podatke programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				// dodaj podatke spremenljivke (prostor + predloga) v podatkovni segment
				List<PDM.DataInstr> data = attrAST.attrData.get(varDef);
				if (data != null)
					dataSegment.addAll(data);
				varDef.inits.accept(this, arg);
				return null;
			}

			@Override
			public Object visit(final AST.AtomExpr atomExpr, final Object arg) {
				// dodaj znake nizovne konstante v podatkovni segment
				List<PDM.DataInstr> data = attrAST.attrData.get(atomExpr);
				if (data != null)
					dataSegment.addAll(data);
				return null;
			}

		}

	}

	// --- ZAGON ---

	/**
	 * Zagon izracuna pomnilniske predstavitve kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (code generation):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				// abstraktna sintaksa:
				final Abstr.AttrAST abstrAttrAST = Abstr.constructAST(synAn);
				// semanticna analiza:
				final SemAn.AttrAST semanAttrAST = SemAn.analyze(abstrAttrAST);
				// pomnilniska predstavitev:
				final Memory.AttrAST memoryAttrAST = Memory.organize(semanAttrAST);
				// generiranje kode:
				final CodeGen.AttrAST codegenAttrAST = CodeGen.generate(memoryAttrAST);

				(new AST.Logger(codegenAttrAST)).log();
				{
					int addr = 0;
					final List<PDM.CodeInstr> codeSegment = (new CodeSegmentGenerator(codegenAttrAST)).codeSegment();
					{
						System.out.println("\n\033[1mCODE SEGMENT:\033[0m");
						for (final PDM.CodeInstr instr : codeSegment) {
							System.out.printf("%8d [%s] %s\n", addr, instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					final List<PDM.DataInstr> dataSegment = (new DataSegmentGenerator(codegenAttrAST)).dataSegment();
					{
						System.out.println("\n\033[1mDATA SEGMENT:\033[0m");
						for (final PDM.DataInstr instr : dataSegment) {
							System.out.printf("%8d [%s] %s\n", addr, (instr instanceof PDM.SIZE) ? " " : instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					System.out.println();
				}
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
