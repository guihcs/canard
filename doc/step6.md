
# step 6

The labels of the subject, predicate and object are retrieved.
Also, the most similar type of the subject predicate and object are retrieved in the class IRITypeUtils on the method *findMostSimilarType*.
This similarity are based in the matched answer and are compared using levenshtein similarity in the class Utils.similarity.