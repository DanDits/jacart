This project is based on the amazing project [GoCart](https://github.com/Flow-Based-Cartograms/go_cart) by [Gastner, Seguy & More](https://www.pnas.org/content/115/10/E2156).

It allows you to transform a list of polygonal regions that are related to some positive numeric value as a [cartogram](https://en.wikipedia.org/wiki/Cartogram#cite_note-GSM-Fast-Flow-Based-26). Those represent the related value by the regions' area, thus need to change the original geometry. We implement area contiguous cartograms that retain the original topology (i.e. the [dual map](https://en.wikipedia.org/wiki/Dual_graph) of the original map and transformed map are isomorphic).

Note: This project is currently WIP and NOT YET FINISHED. Instructions on how to use it, how the API is designed and how to use this project is not yet final. 
This is intended to not only be a port of the C implementation to a JVM based implementation (which will inevitably lead to reduced performance), but most importantly this is about transforming it into something that can be used in a production environment with a clearly designed interface and less reliant on globally passing around pointers.
