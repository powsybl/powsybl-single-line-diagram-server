# PowSyBl Single Line Diagram Server

[![Actions Status](https://github.com/powsybl/powsybl-single-line-diagram-server/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/powsybl/powsybl-single-line-diagram-server/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=com.powsybl%3Apowsybl-single-line-diagram-server&metric=coverage)](https://sonarcloud.io/component_measures?id=com.powsybl%3Apowsybl-single-line-diagram-server&metric=coverage)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Join the community on Spectrum](https://withspectrum.github.io/badge/badge.svg)](https://spectrum.chat/powsybl)
[![Slack](https://img.shields.io/badge/slack-powsybl-blueviolet.svg?logo=slack)](https://join.slack.com/t/powsybl/shared_invite/zt-36jvd725u-cnquPgZb6kpjH8SKh~FWHQ)

Server to generate single line diagram based on spring-boot.

Please read [liquibase usage](https://github.com/powsybl/powsybl-parent/#liquibase-usage) for instructions to automatically generate changesets. 
After you generated a changeset do not forget to add it to git and in src/resource/db/changelog/db.changelog-master.yml
