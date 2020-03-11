# JaCart
This project is based on the amazing project [GoCart](https://github.com/Flow-Based-Cartograms/go_cart) by [Gastner, Seguy & More](https://www.pnas.org/content/115/10/E2156). 
> Gastner MT, Seguy V, More P. Fast flow-based algorithm for creating density-equalizing map projections. Proc Natl Acad Sci USA 115(10):E2156–E2164 (2018)

It allows you to transform a list of polygonal regions that are related to some positive numeric value as a [cartogram](https://en.wikipedia.org/wiki/Cartogram#cite_note-GSM-Fast-Flow-Based-26). Those represent the related value by the regions' area, thus need to change the original geometry. We implement area contiguous cartograms that retain the original topology (i.e. the [dual map](https://en.wikipedia.org/wiki/Dual_graph) of the original map and transformed map are isomorphic). Other cartogram variants like non contiguous Dorling cartograms may follow.

## Examples

 TODO provide some examples, link to blogpost
 
## Project Structure

The project is in a beta phase and has a version 0.2.0 released to maven central under groupId "de.dandit" and is targeted to support Java 11.
It is structured as follows:
- cartogram-core: Basic cartogram logic and API. No external dependencies.
- cartogram-geo: Offers utility methods for converting from/to geotools features, jts geometries and exporting results.

The cartogram-core API is based on the following: Create and supply a [MapFeatureData](cartogram-core/src/main/java/de/dandit/cartogram/core/api/MapFeatureData.java) and configure the execution using a [CartogramConfig](cartogram-core/src/main/java/de/dandit/cartogram/core/api/CartogramConfig.java). For an example usage see the [CartogramApiTest](cartogram-core/src/test/java/de/dandit/cartogram/core/api/CartogramApiTest.java). 
Use the [CartogramApi](cartogram-core/src/main/java/de/dandit/cartogram/core/api/CartogramApi.java) to create the desired cartogram. On success the [CartogramResult](cartogram-core/src/main/java/de/dandit/cartogram/core/api/CartogramResult.java) contains information about the convergence, the transformed regions with their polygons and the used projection.

The cartogram-geo project does not have a clearly defined API yet and is more a utility and convenience library. This may change in future versions.

## Performance and Comments

To give some ideas about convergence performance:
- For usual input without extreme values and a 4 core machine using the commonPool parallelism configuration calculation takes around 2-3 seconds.
- Using higher or lower resolution polygons does not significantly impact the execution time (if the point count does not exceed the chosen grid size which is 512x512 by default).
- Scaling small regions to a very big size or vice versa can cause slow performance or even cause convergence to fail
- The convergence is slower than the C based implementation [GoCart](https://github.com/Flow-Based-Cartograms/go_cart), but not by a lot.
- You can configure and tune the used [FFT](https://en.wikipedia.org/wiki/Fast_Fourier_transform) implementation, though the fourier transformations are not the bottleneck of execution time.

Open TODOs:
- Created polygons can contain self intersections if original polygons were already contained bottlenecks, narrow areas, unlucky fractal coasts or line segments with a relatively long distance line segments. If you require the output to be [valid geometries](https://postgis.net/workshops/postgis-intro/validity.html) you should tune your input to introduce new coordinates within line segments that would be further apart than of the internally used grid cell size.
- Identify bottlenecks and low hanging fruits: The bilinear interpolation needs to be improved or called less frequently. 