# tinder-finder

Search Tinder for a specific person


### Use
Allows you to provide filters (name, gender, age, location, etc) as well as update your own settings (gender, location, etc) to find profiles matching your filters.

### Background
Built off the [Unofficial Tinder API documentation gist](https://gist.github.com/rtt/10403467) with the intention of helping a friend find someone who was impersonating them on Tinder.

You will need Tinder Plus in order to have an unlimited number of swipes, otherwise you can exit after you've run out of likes (~100) and restart 12hrs later.

### [environ](https://github.com/weavejester/environ) vars

```language=clojure
{:fbook-token Facebook auth token (instructions in [gist](https://gist.github.com/rtt/10403467) on how to obtain)
 :fbook-id Accesible in above method but is also the custom id in your facebook url if you added on
 :tinder-token Tinder access token, retrievable with `tinder/get-auth-token` once you have your fbook info set, updated every 24hrs
 :clarifai-token [Clarifai](https://www.clarifai.com/) API token*
 :search-name First name of user you are searching for
 :search-school School of person you are searching for
 :search-bio Bio (full or partial) of person you are searching for
 :search-tags If you are looking to do an image tag comparison you first get the tags of the original photo with Clarifai}
```

### Todo
* real error handling

### Shortcomings
- Built this for my use-case, adjust code as necessary to use different filters than those provided
- Tinder API can change
- Rate-limiting with Tinder `/recs` call
- Using a tool that does true image comparison would provide more accurate results than using just identifiying tags. For my use it was good enough as I had a set of fairly unique search tags and didn't mind filtering through the resulting photos but you can always replace Clarifai with a different API/lib (one SO suggestion explored was [OPENCV](http://docs.opencv.org/2.4/doc/tutorials/introduction/desktop_java/java_dev_intro.html)).
- Clarifai API has a 5k limit/month


Copyright © 2016

Distributed under the [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License](https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode).
