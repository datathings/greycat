# Polynomial compression

Many applications, for example cyber-physical systems or IoT applications, are facing large amounts of rapidly changing data, e.g., measured from sensors. Usually, this data is discretized and stored in timestamp/value pairs. This can easily lead to millions of values in a short amount of time. Besides requiring large amounts of data this makes it also difficult to analyse the large history, e.g., in order to predict future trends. 

Therefore, MWG provides a mechanism, based on machine learning algorithms, which uses polynomials to approximate the timestamp/value signal instead of storing every discrete timestamp/value pair. Machine learning algorithms are applied to learn the polynomial (in live). This significantly improves storage and reasoning costs. The learned polynomial can also be used to predict future trends. 

The following code snipped shows a concrete example.  

```java
Graph g = new GraphBuilder().withPlugin(new MLPlugin()).build();
g.connect(isConnected -> {
	RegressionNode regressionNode = 
		(RegressionNode) g.newTypedNode(0, 0, "PolynomialNode");
	for (int i = 0; i < 100; i++) {
		int finalI = i;
		regressionNode.jump(i, (RegressionNode nodeTi) -> {
		nodeTi.set("value", finalI);
			//nodeTi.learn(finalI,null);
		});
	}

	regressionNode.timepoints(Constants.BEGINNING_OF_TIME,
			Constants.END_OF_TIME, (long[] times) -> {
		System.out.println("Nb of times:" + times.length);
	});
	//print 50.0
	regressionNode.jump(50, n50 -> System.out.println(n50.get("value")));
	//print {"world":0,"time":0,"id":1"polynomial": "-0.0+(1.0*t)"}
	System.out.println(regressionNode.toString());
});
```