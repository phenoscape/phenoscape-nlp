DROP TABLE IF EXISTS `markedupdatasets`.`abbreviations`;
CREATE TABLE  `markedupdatasets`.`abbreviations` (
  `_label` varchar(100) DEFAULT NULL,
  `abbreviation` mediumtext
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `markedupdatasets`.`configtags`;
CREATE TABLE  `markedupdatasets`.`configtags` (
  `tagid` int(5) NOT NULL AUTO_INCREMENT,
  `tagname` varchar(50) DEFAULT NULL,
  `marker` varchar(1) DEFAULT NULL,
  `startStyle` varchar(1) DEFAULT 'N',
  PRIMARY KEY (`tagid`),
  UNIQUE KEY `tagname` (`tagname`)
) ENGINE=InnoDB AUTO_INCREMENT=291 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `markedupdatasets`.`configtype2text`;
CREATE TABLE  `markedupdatasets`.`configtype2text` (
  `firstpara` varchar(10) DEFAULT NULL,
  `leadingIntend` varchar(10) DEFAULT NULL,
  `spacing` varchar(10) DEFAULT NULL,
  `avglength` varchar(10) DEFAULT NULL,
  `pgNoForm` varchar(50) DEFAULT NULL,
  `capitalized` varchar(1) DEFAULT NULL,
  `allcapital` varchar(1) DEFAULT NULL,
  `sectionheading` varchar(50) DEFAULT NULL,
  `hasfooter` varchar(1) DEFAULT NULL,
  `hasHeader` varchar(1) DEFAULT NULL,
  `footerToken` varchar(50) DEFAULT NULL,
  `headertoken` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `markedupdatasets`.`descriptions`;
CREATE TABLE  `markedupdatasets`.`descriptions` (
  `descId` int(11) NOT NULL AUTO_INCREMENT,
  `_order` varchar(50) DEFAULT NULL,
  `section` varchar(50) DEFAULT NULL,
  `start_token` varchar(50) DEFAULT NULL,
  `end_token` varchar(50) DEFAULT NULL,
  `embedded_token` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`descId`)
) ENGINE=InnoDB AUTO_INCREMENT=270 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `markedupdatasets`.`expressions`;
CREATE TABLE  `markedupdatasets`.`expressions` (
  `expId` int(11) NOT NULL AUTO_INCREMENT,
  `_label` varchar(50) DEFAULT NULL,
  `description` mediumtext,
  PRIMARY KEY (`expId`)
) ENGINE=InnoDB AUTO_INCREMENT=247 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `markedupdatasets`.`morpdesc`;
CREATE TABLE  `markedupdatasets`.`morpdesc` (
  `allInOne` varchar(1) DEFAULT NULL,
  `OtherInfo` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `markedupdatasets`.`nomenclatures`;
CREATE TABLE  `markedupdatasets`.`nomenclatures` (
  `nomenID` int(11) NOT NULL AUTO_INCREMENT,
  `nameLabel` varchar(30) DEFAULT NULL,
  `_yes` varchar(1) DEFAULT NULL,
  `_no` varchar(1) DEFAULT NULL,
  `description` varchar(50) DEFAULT NULL,
  `_type` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`nomenID`)
) ENGINE=InnoDB AUTO_INCREMENT=682 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `markedupdatasets`.`ocrstartparagraph`;
CREATE TABLE  `markedupdatasets`.`ocrstartparagraph` (
  `tagid` int(5) NOT NULL AUTO_INCREMENT,
  `paragraph` mediumtext,
  PRIMARY KEY (`tagid`)
) ENGINE=InnoDB AUTO_INCREMENT=115 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `markedupdatasets`.`specialsection`;
CREATE TABLE  `markedupdatasets`.`specialsection` (
  `hasGlossary` varchar(1) DEFAULT NULL,
  `glossaryHeading` varchar(50) DEFAULT NULL,
  `hasReference` varchar(1) DEFAULT NULL,
  `referenceHeading` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;