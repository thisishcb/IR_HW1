# HW1 Probabilistic Ranking

## parse up query
in `Main`  
union of the Title and Description fields as your query.  
unpaired tags  
```html
<top>

<num> Number: 351 
<title> Falkland petroleum exploration 

<desc> Description: 
XXXXXXXXX

<narr> Narrative: 
XXXXXXXXX

</top>
```
## Indexer
```shell
java -cp ./out/artifacts/HW1_index_jar/HW1.jar IndexFiles \
  -index testdata/index -docs testdata/text
```

original Indexer only index the filepath  
New Indexer: read files and add each document,
with fields: **Path, DOCID, DOCNO, TITLE ,TEXT**  
*DOCID* starts with 1, count doc by number  
*DOCNO* is unique document number come with the file e.g. "FT911-1"  
*TITLE* is title
*TEXT* is the concatenation of doc title and body, ignore paragraphs

#### Common Fields
```
FT: DOCNO   DATE      HEADLINE    TEXT
FB: DOCNO   DATE1     TI          TEXT
LA: DOCNO   DATE<p>   HEADLINE<p> TEXT<p> 
```

## Searcher
### BM25
### LM
### RM1
### RM3