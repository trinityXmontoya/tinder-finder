# tinder-finder


# Background
Built off the [Unofficial Tinder API documentation gist](https://gist.github.com/rtt/10403467) with the intention of helping a friend find someone who was impersonating them on Tinder

### env vars (using [environ](https://github.com/weavejester/environ) with a `profiles.clj`)
:fbook-token Facebook auth token (instructions in [gist](https://gist.github.com/rtt/10403467) on how to obtain)
:fbook-id Accesible in above method but is also the custom id in your facebook url if you added on
:tinder-token Tinder access token, retrievable with `tinder/get-auth-token` once you have your fbook info set
:clarifai-token [Clarifai](https://clarifai.com/) API token*
:search-name Name of user you are searching for
:search-school School of person you are searching for
:search-bio Bio (full or partial) of person you are searching for
:search-tags If you are looking to do an image comparison you first get the tags of the *



 * only necessary if you are looking to do image comparison, retrieve token after [signing up with Clarifai](https://developer.clarifai.com/). 5k limit/month.


## Shortcomings
- Tinder API can change
- Using a tool that does true image comparison would provide more accurate results than simply using search tags. For my use it was good enough as I didn't mind filtering through the resulting photos but you can always replace Clarifai with a different API/lib.

Copyright © 2016 FIXME
