# Banias

![](banias-logo-lowres-trimmed.png)

[![License](https://img.shields.io/github/license/doitintl/zorya.svg)](LICENSE) [![GitHub stars](https://img.shields.io/github/stars/doitintl/banias.svg?style=social&label=Stars&style=for-the-badge)](https://github.com/doitintl/banias) [![Build Status](https://secure.travis-ci.org/doitintl/banias.png?branch=master)](http://travis-ci.org/doitintl/banias)


Banias (Arabic: بانياس الحولة‎; Hebrew: בניאס‬) is the Arabic and modern Hebrew name of an ancient site developed around a river once associated with the Greek god [Pan](https://www.wikiwand.com/en/Pan_(mythology)).

So, like the flow of the Banias, events are flowing into our system. So we decided to build a reference architecture and actual implementation of event analytics pipeline. You can take the code as it is and use it or use it a design reference.

Banias Architecture:
* API receiving the events data from the producers (e.g. web apps, mobile app or backend servers)
* The events are sent to Google Pub/Sub topic
* Apache Beam/Google Cloud Dataflow streams the events into BigQuery with or without mutations or agregations

![](Banias_Architecture.png)

## Installation

### [API Deployment](frontend/README.md)

### [Apache Beam/Google Cloud Dataflow Deployment](backend/README.md)
