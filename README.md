# Highlighter #

Originally a code test for a job interview. Given two arguments, a document and a query, the program returns a snippet from the document that's most relative to the given query, with both 'relevant' and 'snippet' being subjective and purposely vague in the original project description.

package: herrick.test

See Highlighter.java or below for notes/rants.

## Build Options:

Run these from the root project directory (containing build.xml):

	$ ant compile  # compiles!

	$ ant run 	   # runs whatever's in Highlighter's main()
	
	$ ant test 	   # runs through tests in Tests (uses JUnit)
	  	  		   # output (upon failure) will be in .txt in base dir

	$ ant clean    # removes everything in bin/ and the dir itself

## Notes

### Assumptions

- using ASCII alphabet 
- ignoring:
  - punctuation
  - capitalization
  - excess whitespace (including the returned snippet)

***
### OVERVIEW

The program splits the given strings into char arrays, which are
then searched with the Boyer-Moore algorithm.  During searches, the
number of matches(weights) are summed on a per-word basis.  The 
logic/weighting	is best explained by example:

#### Examples

doc = "toast toaster toad"

__CASE 1__:  query = "toast"  
_Weights after search_:  
toast: 1  
toaster: 1  
toad: 0  

__CASE 2__:  query = "toaster"  
toast: 0  
toaster: 1  
toad: 0  

__CASE 3__:  query = "toast toaster"  
toast: 2  
toaster: 2  
toad: 0  

__CASE 4__:  query = "\"toast toaster\""  
toast: 2  
toaster: 2  
toad: 0  

__CASE 5__:  query = "\"toaster toast\""  
toast: 1  
toaster: 2  
toad: 0  
 
Cases 3 and 4 produce the same weights because the algorithm
will first attempt to match a non-phrase search as a phrase 
search, and if possible will value it doubly as well.

In Case 5, the query  would fail to match completely, and try
again without quotes.

A query of "\"toaster\"" or "\"toast\"" would be similar to 
cases 1 and 2, respectively, except each weight would be doubled.
Effectively, a single-word phrase search behaves the same as 
a single-word search, but weights are doubled.

Once searches are completed, the Weights[] array is examined 
to find the single contiguous snippet with the highest total
weight, such that #chars <= SNIP_SIZE.

The highlight() function then takes that snippet and inserts
highlighting tags between words, based on whether that word 
has weight > 0.  Since highlighting/snipping is done on whole
words rather than characters, punctuation adjacent to a 
highlighted word will be included in the tag.
 

### ARBITRARY CHOICES
- returns first snippet in doc if there's no match or no query
- returns HIGHLIGHTED first snippet if doc==query
- returns null if doc is an empty string or null


####   WHAT IT DOES DO:
#####	 SEARCHING:
- Always tries to find exactly what you typed first 
- Removes common terms, unless you specified to keep them
- Removes duplicate terms, ""   "" 
- Ignores case

#####	 MATCHING
- Weights matches based on quotes / order of typing
- Matches partials (see above examples)
 - EX:Allows you to match/highlight 'automobile',
	  'autopilot' by  searching for 'auto'
#####	 HIGHLIGHTING
- Efficiently encloses multiple search terms in a single tag
  - no [/tag][tag] cases
#####	 RETURNING
- Returns 'best' snippet based on #enclosing matches
- Keeps original formatting save for whitespace
  - Won't have a \n in the middle of the snippet
#####	 ADJUSTMENTS
- Can easily set SNIP_SIZE, ALPHA_SIZE

####  WHAT IT DOESN'T DO:
##### Partially-highlighted words, non-highlighted punctuation
- As noted above, this is due to how I highlight.
  partial words/phrases do in fact match, but 
  the entire word/phrase is highlighted.

##### Matches multiple phrases
- Due to how I count matches, and how I fall through 
	certain conditions, it would take some reworking to 
	manage this.  As of right now it just finds the first
	phrase, and regards the rest as normal terms.
	- It wouldn't be THAT hard to implement, as the Pattern
	and Matcher are already there. 


##### Removes common words from document for faster searching
- Rebuilding becomes a problem unless we keep track of 
  what was removed, which doesn't seem worth it.
##### Any kind of 'smart' processing
- No NLP, autocompletion, related terms/searches, etc.

#### FUTURE IDEAS
 - re-implement entirely with char[]s.  No String[]s.
  - this would eliminate the need for many proprietary
	instance structures used here and save on both 
	space and time.
 - Append to jump table, instead of reinitializing after every word/phrase

## Dependencies 
- JUnit for Unit testing
- Ant (if you want to use my build file)




Thanks for reading all that.  You did read it, right?

