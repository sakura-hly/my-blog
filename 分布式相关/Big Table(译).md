# Big Table: A Distributed Storage System For Structured Data

## 摘要

Bigtable是用于管理结构化数据的分布式存储系统，该系统旨在扩展到非常大的规模：数千个商用服务器中的PB级数据。 Google的许多项目都将数据存储在Bigtable中，包括网络索引，Google Earth和Google Finance。 这些应用程序在数据大小（从URL到网页到卫星图像）和延迟要求（从后端批量处理到实时数据服务）方面都对Bigtable提出了截然不同的要求。 尽管有各种各样的要求，Bigtable还是为所有这些Google产品成功提供了一种灵活的高性能解决方案。 在本文中，我们描述了Bigtable提供的简单数据模型，该模型为客户提供了对数据布局和格式的动态控制，并描述了Bigtable的设计和实现。
