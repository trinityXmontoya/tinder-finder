# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## 0.1.0-SNAPSHOT - 2016-09-13/14
### Added
- Tinder API functionality needed to update profile to search (new location, gender, filters (gender, age, distance))
- Ability to filter through results based off name, bio, or school matches
- 'Pass' on mismatches + 'Like' matches so they don't come up in later calls to '/users/recs'
- Skeleton to write results to Redis for later querying
- Use [Clarifai](https://clarifai.com/#demo) API to match images to desired search tags
- Save off all primary photo search tags for future (unrelated) analysis


[Unreleased]: https://github.com/your-name/tinder-finder/compare/0.1.1...HEAD
[0.1.1]: https://github.com/your-name/tinder-finder/compare/0.1.0...0.1.1
