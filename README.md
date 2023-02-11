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
java -jar HW1.jar IndexFiles testdata/index testdata/text
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

example query string  
`"Falkland petroleum exploration What information is available on petroleum exploration in the South Atlantic near the Falkland Islands?"`

parser: add escape char to all special characters in the query text
since they are not meant to be combination.
### BM25
```shell
java -jar HW1.jar BM25 testdata/index testdata/eval/topics.351-400 testdata/BM25result.txt
```
Modified from demo default Searcher  
change the similarity scoring to `org.apache.lucene.search.similarities.BM25Similarity`
with default parameters $k1=1.2$, $b=0.75$

### LM
```shell
java -jar HW1.jar LM testdata/index testdata/eval/topics.351-400 testdata/LMresult.txt
```
same as BM25, but change the `BM25Similarity` to `LMDirichletSimilarity`

### RM1
```shell
java -jar HW1.jar RM1 testdata/index testdata/eval/topics.351-400 testdata/RM1result.txt
```
1. Retrieve Initial 1k documents of query $Q$ with `LMDirichletSimilarity` as relevant docuements $R$
2. Expand the query terms based on term frequencies from $R$, get expanded terms $t$
3. get the weight of each document as $P(t|D)QL(q|D)$
4. normalize the weights as sum up to 1
5. re-rank the documents and output

### RM3
```shell
java -jar HW1.jar RM3 testdata/index testdata/eval/topics.351-400 testdata/RM3result.txt
```
based on RM3, instead using the normalized $P(t|D)QL(q|D)$ as weight, introduce $\lambda$,
$\lambda=0.5$ by default, 
The new weight is calculated as $(1-\lambda)P_{MLE}(t|q) + \lambda P(t|q)$ 
where the MLE is calculated based on the original query