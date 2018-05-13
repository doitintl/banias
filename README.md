# Banias

![](banias-logo-lowres-trimmed.png)

[![License](https://img.shields.io/github/license/doitintl/zorya.svg)](LICENSE) [![GitHub stars](https://img.shields.io/github/stars/doitintl/banias.svg?style=social&label=Stars&style=for-the-badge)](https://github.com/doitintl/banias) [![Build Status](https://secure.travis-ci.org/doitintl/banias.png?branch=master)](http://travis-ci.org/doitintl/banias)


Banias (Arabic: بانياس الحولة‎; Hebrew: בניאס‬) is the Arabic and modern Hebrew name of an ancient site that developed around a spring once associated with the Greek god [Pan](https://www.wikiwand.com/en/Pan_(mythology)).

And like the flow of the Banias events are flowing into our system. So we decided to build a reference architecture and actual implementation of event analytics pipeline. You can take the code as it is and use it or use it a design reference.

The architecture is as follows:
* Front end application that receives all the events from the producers (could be web apps, mobile app or backend servers)
* The events are entered into google pub/sub
* Using dataflow the events are streamed into BigQuery

![](Banias_Architecture.png)

## Installtion

### [FrontEnd](frontend/README.md)

### [BackEnd](backend/README.md)
