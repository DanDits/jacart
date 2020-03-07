This project is based on the amazing project [GoCart](https://github.com/Flow-Based-Cartograms/go_cart) by [Gastner, Seguy & More](https://www.pnas.org/content/115/10/E2156). 
> Gastner MT, Seguy V, More P. Fast flow-based algorithm for creating density-equalizing map projections. Proc Natl Acad Sci USA 115(10):E2156–E2164 (2018)

It allows you to transform a list of polygonal regions that are related to some positive numeric value as a [cartogram](https://en.wikipedia.org/wiki/Cartogram#cite_note-GSM-Fast-Flow-Based-26). Those represent the related value by the regions' area, thus need to change the original geometry. We implement area contiguous cartograms that retain the original topology (i.e. the [dual map](https://en.wikipedia.org/wiki/Dual_graph) of the original map and transformed map are isomorphic).

Note: This project is currently WIP and NOT YET FINISHED. Instructions on how to use it, how the API is designed and how to use this project is not yet final. 
This is intended to not only be a port of the C implementation to a JVM based implementation (which will inevitably lead to reduced performance), but most importantly this is about transforming it into something that can be used in a production environment with a clearly designed interface and less reliant on globally passing around pointers.

TODOs:
- Created polygons can contain self intersections if original polygons were already contained bottlenecks, narrow areas, unlucky fractal coasts or line segments with a relatively long distance line segments
- Identify bottlenecks and low hanging fruits
- Write tests and support edge cases (e.g. no or negative value for a region, skipping of small parts of a multi geometry, simplification of a geometry to reduce chance for self intersections,...)
- Deploy on MavenCentral 
- Analyse and compare with C implementation in regards to execution speed, memory usage and usability
- When neighbored polygons share a border but share no corner point then the topology is not/can not be retained (sample3). Maybe automatically create artificial intermediate corner points on the segment for that (sample4).