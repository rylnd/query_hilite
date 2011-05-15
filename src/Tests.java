package herrick.test;
	import junit.framework.*;
	
	public class Tests extends TestCase { 
	  private Highlighter h;
		private String st = "[[HIGHLIGHT]]";
		private String end = "[[ENDHIGHLIGHT]]";
		private String word = "something";
		private String words = "something else entirely";
		private String under200 = "This is an example sentence. I am using it for testing purposes. It is nice and pretty and contains work-appropriate words and phrases. This one is under 200 characters.";

		private String doc = "toast toaster toad";

	  public Tests(String name) {
	    super(name);
	  }
	
	  protected void setUp() { 
			h = new Highlighter();
	  }

	  protected void tearDown() { 
			h = null;
	  }
	
		//test initialization 
	  public void testInit() {
			assertNotNull(h);
	  }
		//---BASE CASES
		public void testEmptyDoc(){
			assertNull(h.highlight_doc("", word));
		}

		public void testEmptyQuery1(){
			assertEquals(word, h.highlight_doc(word, ""));
		} 

		public void testEmptyQuery2(){
			assertEquals(words, h.highlight_doc(words, ""));
		} 
		
		public void testNoMatch(){
			assertEquals(under200, h.highlight_doc(under200, "excelsior"));
		}
		
	 	public void testFullMatch(){
			assertEquals(st+under200+end, h.highlight_doc(under200, under200));
		}

		public void testLongerQuery(){
			String doc1 = "this is a test";
			String query1 = "this is a longer test";
			assertEquals(st+"this"+end+" is a "+st+"test"+end,
									 h.highlight_doc(doc1, query1));

		}
		
		public void test1highlight(){
			assertEquals(st + word + end,
									 h.highlight_doc(word, word));
		}

		public void testmulthighlight(){
			assertEquals(st + words + end,
									 h.highlight_doc(words, words));
		}

		//TESTS FOR RMCOMMON()
		public void testRmCommon(){
			assertEquals("", h.rmCommon("i is from the it"));
			assertEquals("this test", h.rmCommon("this is a test."));
			assertEquals("this last test have", 
									 h.rmCommon("This is the last test I have."));
		}
		
		//TESTS FOR RMDUPES()
		public void testRmDupes(){
			assertEquals("this is a test", 
									 h.rmDupes("This this is is a a test test."));
			assertEquals("this is a test", 
									 h.rmDupes("This is a test. This is a test."));
			
		}

		//Some tests to see if weights are being filled correctly
		//Basically we'll just test the examples I gave in
		//Highlighter.java

		public void testWeights1(){
			h.highlight_doc(doc, "toast");
			assertEquals("toast: ", 1, h.Weights[0]);
			assertEquals("toaster: ", 1, h.Weights[1]);
			assertEquals("toad: ", 0, h.Weights[2]);
		}

		public void testWeights2(){
			h.highlight_doc(doc, "toaster");
			assertEquals("toast: ", 0, h.Weights[0]);
			assertEquals("toaster: ", 1, h.Weights[1]);
			assertEquals("toad: ", 0, h.Weights[2]);
		}

		public void testWeights3(){
			h.highlight_doc(doc, "toast toaster");
			assertEquals("toast: ", 2, h.Weights[0]);
			assertEquals("toaster: ", 2, h.Weights[1]);
			assertEquals("toad: ", 0, h.Weights[2]);
		}

		public void testWeights4(){
			h.highlight_doc(doc, "\"toast toaster\"");
			assertEquals("toast: ", 2, h.Weights[0]);
			assertEquals("toaster: ", 2, h.Weights[1]);
			assertEquals("toad: ", 0, h.Weights[2]);
		}
		
		public void testWeights5(){
			h.highlight_doc(doc, "\"toaster toast\"");
			assertEquals("toast: ", 1, h.Weights[0]);
			assertEquals("toaster: ", 2, h.Weights[1]);
			assertEquals("toad: ", 0, h.Weights[2]);
		}
		
		
		public void testWeights6(){
			h.highlight_doc(doc, "\"toaster\"");
			assertEquals("toast: ", 0, h.Weights[0]);
			assertEquals("toaster: ", 2, h.Weights[1]);
			assertEquals("toad: ", 0, h.Weights[2]);
		}
		
		public void testCommons(){
			//test that commons are kept in a phrase
			assertEquals(st + "this and that" + end, 
									 h.highlight_doc("this and that", 
																	 "\"this and that\""));
			//test that commons are removed from a non-phrase
			//(unless query.equals(doc))
			assertEquals(st +"this"+end+ " and " +st+ "that" + end + " and", 
									 h.highlight_doc("this and that and", 
																	 "this and that"));

		}

		/* NOTE: these are trickier because we'll still get the 
		 * same highlighting whether we remove duplicates
		 * from the query or not (in small cases)
		 * Therefore, we'll have to check the resulting 
		 * weights to be certain
		*/
		public void testDupes1(){
			//test that dupes are kept in a phrase
			h.highlight_doc("this and this and",  "\"this and this\"");
			assertEquals(2 ,h.Weights[0]);
			assertEquals(2 ,h.Weights[1]);
			assertEquals(2 ,h.Weights[2]);
		}

		public void testDupes2(){
			//test that dupes are removed from a non-phrase
			h.highlight_doc("this and this and",  "this and this");
			assertEquals(1 ,h.Weights[0]);
			assertEquals(0 ,h.Weights[1]);
			assertEquals(1 ,h.Weights[2]);
		}


	}
