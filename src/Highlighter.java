// CODE TEST, SUMMER 2010 BY RYLAND HERRICK
//
//	SEARCH ASSUMPTIONS:
//	-using ASCII alphabet 
//	-ignoring:
//	  - punctuation
//		- capitalization
//		- excess whitespace (including the returned snippet)
//
//
//	OVERVIEW
//	This program splits the given strings into char arrays, which are
//	then searched with the Boyer-Moore algorithm.  During searches, the
//	number of matches(weights) are summed on a per-word basis.  The 
//	logic/weighting	is best explained by example:
//	
//	doc = "toast toaster toad"
//	CASE 1: query = "toast"     CASE 3: query = "toast toaster"
//	 -Weights after search:             toast: 2
//	 toast: 1                           toaster: 2
//	 toaster: 1                         toad: 0
//	 toad: 0
//	CASE 2: query = "toaster"   CASE 4: query = "\"toast toaster\""
//	 -Weights after search:             toast: 2
//	 toast: 0                           toaster: 2 
//	 toaster: 1                         toad: 0
//	 toad: 0
//
//	CASE 5: query = "\"toaster toast\""
//	 toast: 1
//	 toaster: 2
//	 toad: 0
//	 
//	 Cases 3 and 4 produce the same weights because the algorithm
//	 will first attempt to match a non-phrase search as a phrase 
//	 search, and if possible will value it doubly as well.
//
//	 In Case 5, the query  would fail to match completely, and try
//	 again without quotes.
//	 
//	 A query of "\"toaster\"" or "\"toast\"" would be similar to 
//	 cases 1 and 2, respectively, except each weight would be doubled.
//	 Effectively, a single-word phrase search behaves the same as 
//	 a single-word search, but weights are doubled.
//
//	 Once searches are completed, the Weights[] array is examined 
//	 to find the single contiguous snippet with the highest total
//	 weight, such that #chars <= SNIP_SIZE.
//
//	 The highlight() function then takes that snippet and inserts
//	 highlighting tags between words, based on whether that word 
//	 has weight > 0.  Since highlighting/snipping is done on whole
//	 words rather than characters, punctuation adjacent to a 
//	 highlighted word will be included in the tag.
//	 
//
//	NOTES/ARBITRARY CHOICES
//	-returns first snippet in doc if there's no match or no query
//	-returns HIGHLIGHTED first snippet if doc==query
//	-returns null if doc is an empty string or null
//
//
// SUMMARY:
//   WHAT IT DOES DO:
//	 SEARCHING:
//	 -Always tries to find exactly what you typed first
//	 -Removes common terms, unless you specified to keep them
//	 -Removes duplicate terms, ""   "" 
//	 -Ignores case
//	 MATCHING
//	 -Weights matches based on quotes / order of typing
//	 -Matches partials (see above examples)
//	  - EX:Allows you to match/highlight 'automobile',
//		  'autopilot' by  searching for 'auto'
//	 HIGHLIGHTING
//	 -Efficiently encloses multiple search terms in a single tag
//	   -no [/tag][tag] cases
//	 RETURNING
//	 -Returns 'best' snippet based on #enclosing matches
//	 -Keeps original formatting save for whitespace
//	   -Won't have a \n in the middle of the snippet
//	 ADJUSTMENTS
//	 -Can easily set SNIP_SIZE, ALPHA_SIZE
//
//
//  WHAT IT DOESN'T DO:
//	-Partially-highlighted words, non-highlighted punctuation
//	  - As noted above, this is due to how I highlight.
//		  partial words/phrases do in fact match, but 
//			the entire word/phrase is highlighted.
//
//	-Matches multiple phrases
//	  -Due to how I count matches, and how I fall through 
//		certain conditions, it would take some reworking to 
//		manage this.  As of right now it just finds the first
//		phrase, and regards the rest as normal terms.
//		-It wouldn't be THAT hard to implement, as the Pattern
//		and Matcher are already there. 
// >>>NOTE: If this feature would make or break me getting
//    a phone interview (however unlikely that case may be),
//		give me 2 hours and I'll implement it for you.
//
//
//	-Removes common words from document for faster searching
//	  - Rebuilding becomes a problem unless we keep track of 
//		  what was removed, which doesn't seem worth it.
//	-Any kind of 'smart' processing
//	  - No NLP, autocompletion, related terms/searches, etc.
//
//		FUTURE IDEAS - 
//		 -re-implement entirely with char[]s.  No String[]s.
//		  -this would eliminate the need for many proprietary
//			instance structures used here and save on both 
//			space and time.
//
//
//		Thanks for reading all that.  You did read it, right?
//
//

package herrick.test;

import java.util.*;
import java.util.regex.*;

public class Highlighter {

	protected static final String[] COMMONS = {"a", "and", "be", "for", 
  "from", "has", "i", "in", "is", "it", "of", "on", "to", "the"};

	// Pattern/Matcher Objects for finding a quoted phrase: "phrase here"
	protected static final Pattern PHRASE_PATTERN = 
		Pattern.compile("\"[^\"\r\n]*\"");
	protected Matcher phrMatch;
	protected String phrQuery;

	// array to hold #matches/weights of each word
	protected int[] Weights;

	protected int ALPHA_SIZE = 128; // ASCII alphabet size
	protected int SNIP_SIZE = 200; // size of final snippet in chars
	protected int docWords; // words in document
	protected int qWords; // holds words in LATEST search

	// Arrays for building / searching document
	protected String[] origDocA, searchDocA;

	// Map for <index in doc, index in array>
	protected HashMap<Integer, Integer> indexMap;

	// matchesV:  Vector of Integer[2]s.
	// Each item I in the vector corresponds to a match as found
	// by boyerMooreSearch.
	// I[0] = index of first character in the document that was searched.
	//       NOTE: IN PRESENT CASE THIS IS NOT THE ORIGINAL DOCUMENT.
	//            IT IS THE FORMATTED, SEARCHABLE ONE.
	// I[1] = number of words that this match contained.
	//        If > 1 will be weighted doubly.
	//
	protected Vector<Integer[]> matchesV;

	// search-specific arrays for our search parameters
	protected char[] needleA, haystackA;
	
	// search-specific lengths of our search parameters
	protected int needleLen, haystackLen;

	// search-specific tables for how far ahead we should skip
	protected int[] badCharA, goodSuffA, suff;




	public static void main(String[] args){}

	public Highlighter(){}

	public void setSnip(int snip){
		this.SNIP_SIZE = snip;
	}

	public void setAlpha(int alpha){
		this.ALPHA_SIZE = alpha;
	}

	public String highlight_doc(String doc, String query){

		// BASE CASE
		if(doc==null || doc.length()==0) return null;

		// now, we want to format our inputs (doc, query)
		
		// split document on whitespaces, keep for building snippet
		origDocA = doc.split("\\s+");
		docWords = origDocA.length;

		// MORE BASE CASES
		if(query==null || query.length()==0) 
			return noMatch();
		if(format(doc).equals(format(query))) 
			return "[[HIGHLIGHT]]" +noMatch()+"[[ENDHIGHLIGHT]]";

		Weights = new int[docWords];
		indexMap = new HashMap<Integer, Integer>(docWords);

		// format for searching - remove punctuation, excess whitespace
		doc = format(doc);
		searchDocA = doc.split("\\s+");
		query = format_Q(query);
		
		// regrettably, iterate through to fill Map
		indexMap_init();

		// Check to see if query has explicit phrase(s)
		phrMatch = PHRASE_PATTERN.matcher(query);
		if(phrMatch.find()){ //found a phrase 

			// extract phrase query
			phrQuery = rmPunc(query.substring(phrMatch.start(), phrMatch.end()));

			// don't care about punctuation now, remove from query
			query = rmPunc(query);

			if(boyerMooreSearch(doc, phrQuery)){// we can match our phrase, use it
				fillWeights(2);
				if(needleLen!=query.length()){// other words in query
					String rest = rmDupes(rmCommon(getRest(query, phrQuery)));
					searchWords(doc, rest);
				}
			}
			else{// can't match phrase, ignore quotes
				if(!searchWords(doc, rmDupes(rmCommon(query)))){
					return noMatch();
				}
			}
		}
		else{ // no phrases, but try to match exactly as typed first(less commons)
			query = rmDupes(rmCommon(rmPunc(query)));
			if(boyerMooreSearch(doc, query)){// found an exact match
				if(qWords > 1)
					fillWeights(2);
				else
					fillWeights(1);
			}
			else{// search normally
				if(!searchWords(doc, query)){
					return noMatch();
				}
			}
		}
		// done searching, find best snippet
		int[] snip = bestSnippet();
		return highlight(snip);
	}

	// STRING HELPER FUNCTIONS
	//		 I realize these aren't the most efficient way to do things, 
	//		 but they're straightforward and are only called for 
	//		 preformatting of the inputs
	//

	// strips the punctuation, capitalization, excess whitespace
	protected String format(String input){
		return input.replaceAll("\\p{Punct}+", "")
			.replaceAll("\\s+", " ").toLowerCase();
	}
	
	// same as format, but leaves double quotes
	protected String format_Q(String input){
		return input.replaceAll("[\\p{Punct}&&[^\"]]+", "")
			.replaceAll("\\s+", " ").toLowerCase();
	}

	// strips all ASCII punctuation
	protected String rmPunc(String input){
		return input.replaceAll("\\p{Punct}+", "");
	}


	// returns a String with the given String removed
	protected String getRest(String input, String remove){
		return input.replace(remove, "").trim();
	}


	// Returns final String(snippet) with tags inserted.
	//	  Words are tagged according to whether
	//	  their value in Weights[] > 0.
	//	 
	protected String highlight(int[] snip){
		boolean inProgress = false;
		String snippet = "";
		String strt = "[[HIGHLIGHT]]";
		String end = "[[ENDHIGHLIGHT]]";

		for(int i = snip[0]; i <=snip[1]; i++){
			if(Weights[i]!=0)
				if(inProgress)
					snippet += " " +origDocA[i];
				else{
					snippet += " " +strt+origDocA[i];
					inProgress=true;
				}
			else 
				if(inProgress){
					snippet = snippet.trim();
					snippet += end + " " +origDocA[i]+ " ";
					inProgress=false;
				}
				else
					snippet += " " +origDocA[i]+ " ";
		}
		
		if(inProgress)// need an end-highlight
			snippet += end;
		return snippet.replaceAll("\\s+", " ").trim();
	}

	// Returns the first possible snippet.
	//	  Called when there is either no match
	//	  or no query.  In reality this should never
	//	  be called, but here for testing.
	//	 
	protected String noMatch(){
		String rtn = "";
		String temp = "";
		int i, length;
		i = length = 0;
		temp = origDocA[i];
		rtn += temp + " ";

		while(length + temp.length() + 1 <= SNIP_SIZE && i < docWords-1){
			temp = origDocA[++i];
			rtn += temp + " ";
		}
		return rtn.trim();
	}


	// Method for removing common words (contained in COMMONS[])
	//	  from a given string.
	//	 
	protected String rmCommon(String input){
		String temp = format(input);
		for(String x : COMMONS){
			temp = temp.replaceAll("\\b" +x+ "\\b", "");
		}
		return temp.replaceAll("\\s+", " ").trim();
	}


	// Removes duplicate words from a given 
	//	  input string.  Returns the new
	//	  String.
	//	 
	protected String rmDupes(String input){
		String output = "";
		String[] temp = format(input).split("\\s+");
		Vector<String> vec = new Vector<String>(temp.length);
		for(String x : temp){
			if(!vec.contains(x)){
				vec.add(x);
				output += x + " ";
			}
		}
			return output.trim();
	}

	// END STRING HELPER FUNCTIONS
	//




	//  Take the data gathered from a 
	//	boyerMooreSearch (currently in matchesV) and add it to the 
	//	'relevance values' array, Weights[]
	//
	protected void fillWeights(int weight){
		Integer k, j, index, num;
		for(Integer[] i : matchesV){
			j = i[0];
			num = i[1];
			index = indexMap.get(j);
			Weights[index] += weight;
			for(k = 0; k < num-1; k++){
				j += origDocA[index+k].length()+1;
				Weights[indexMap.get(j)] += weight;
			}
		}
	}


	//  Called after Weights[] is filled, and searching is done.
	//	    Calculates the highest-weighted snippet of
	//	    size SNIP_SIZE (plus punctuation)
	//			and returns the corresponding indices of the original array
	//
	protected int[] bestSnippet(){
		int[] rtn = new int[2];
		String temp; 
		int sum, length, i, head, tail;
		i = sum = length = head = tail = 0;
		int max =-1;
		temp = searchDocA[i];
		// initial loop/sum, first possible snippet
		while(length + temp.length() + 1 <= SNIP_SIZE && i < docWords){
			sum += Weights[i];
			length += temp.length() + 1;
			rtn[1] = i++;
			if(i < docWords)
				temp = searchDocA[i];
		}
		head = rtn[1];
		max = sum;
		
		while(i < docWords){
			do{// shift 'queue'
				sum -= Weights[tail];
				length -= searchDocA[tail].length() +1;
				tail++;
			}while(length + temp.length()+1 > SNIP_SIZE);
		

			sum += Weights[i];
			length += temp.length() + 1;
			head = i++;
			if(sum > max){
				rtn[0] = tail;
				rtn[1] = head;
			}
			if(i < docWords)
			temp = searchDocA[i];
		}
		return rtn;
	}

	// searches the doc on each individual term remaining.
	// fills in weights if necessary.
	protected boolean searchWords(String doc, String rest){
		String[] words = rest.split("\\s+");
		boolean isMatched = false;
		for(String word : words){
			if(boyerMooreSearch(doc, word)){
				 fillWeights(1);
				 isMatched = true;
			}
		}
		return isMatched;
	}

	// initializes the map from:
	// (index in doc)=(index in array)
	protected void indexMap_init(){
		int temp1 = 0;
		int temp2 = 0;
		for(String str : searchDocA){
			indexMap.put(temp1, temp2);
			temp1 += str.length() + 1;
			temp2++;
		}
	}



	//--MY IMPLEMENTATION OF THE BOYER-MOORE ALGORITHM.--

	
	// Actual searching function, implementing the 
	//		Boyer-Moore searching algorithm.
	//		Returns true if a match was found, false otherwise.
	//
	protected boolean boyerMooreSearch(String doc, String query) {
		boolean isMatched = false;
		qWords = query.split("\\s+").length;
		needleA = query.toCharArray();
		haystackA = doc.toCharArray();
		needleLen = needleA.length;
		haystackLen = haystackA.length;
		
		badCharA = new int[ALPHA_SIZE];
		goodSuffA = new int[needleLen + 1];
		suff = new int[needleLen + 1];
		
		// calculate skip tables
		badCharA_init();
		goodSuffA_init();

		// to prevent resizing costs, set size
		matchesV = new Vector<Integer[]>(docWords);
		int i, j, k;

		i = 0;
		while(i <= haystackLen - needleLen){
			j = needleLen-1;

			// loop from right to left, while chars match
			while(j >= 0 && needleA[j]==haystackA[i+j]) j--;
			
			if(j < 0){// isMatched whole pattern, add to matches
				isMatched = true;
				// START UGLY HACK
				// if we have a partial-word match, find index of word-start
				Integer[] match = new Integer[2];
				match[1] = new Integer(qWords);
				match[0] =  new Integer(i);
				if(indexMap.containsKey(match[0]))
					 matchesV.add(match);
				else{
					k = i;
					while(!indexMap.containsKey((Integer)k))	k--;
					match[0] = new Integer(k);
					matchesV.add(match);
				}
				// END UGLY HACK
				// increment search to next possible match
				i += goodSuffA[0];
			}
			else{// didn't match whole pattern, so jump by largest skip
				i += Math.max(goodSuffA[j+1], 
											j - badCharA[haystackA[i+j]]);
			}
		}
		return isMatched;
	}

	// Easier of the two table calculations, makes the BM structure commonly
	//	  referred to as the 'bad character shift' table.
	//	  Works exactly as described on Wikipedia. (where I got it!)
	//	 
	protected void badCharA_init(){

		for(int i = 0; i < ALPHA_SIZE; i++)
			badCharA[i] = -1;

		for(int j = 0; j< needleLen; j++)
			badCharA[ needleA[j] ] = j;
	}

	//  More complicated of the two table initializations.
	//	  Where Boyer-Moore gets its speed.
	//	 	Fills goodSuffA[], a table that holds the following:
	//	 	On a mismatch, goodSuffA[j] contains
	//	  how far ahead we can jump in our search, given that
	//	  we had matched j chars of our query before failing.
	//
	protected void goodSuffA_init(){
		// suff[i] contains the start index of the widest suffix border
		// of the pattern starting at i
		int i=needleLen, j=needleLen+1;
    suff[i]=j;
    while (i>0)
			{
        while (j<=needleLen && needleA[i-1]!=needleA[j-1])
					{
            if (goodSuffA[j]==0) goodSuffA[j]=j-i;
            j=suff[j];
					}
        i--; j--;
       suff[i]=j;
			}

		j = suff[0];
		for(i = 0; i <= needleLen; i++){
			if(goodSuffA[i]==0)
				goodSuffA[i] = j;
			if(i==j)
				j = suff[j];
		}
	}
}
