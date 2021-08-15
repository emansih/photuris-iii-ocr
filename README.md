### Photuris III

Currently this is a Minimum Viable Product(MVP) for a service I am providing - An Optical Character Recognition(OCR) for scanning receipts for 
[Photuris III](https://github.com/emansih/FireflyMobile). I am not able to integrate this software into the main app as it has 3rd party proprietary services / software.

They are: 
- [Microsoft Azure Computer Vision](https://azure.microsoft.com/en-us/services/cognitive-services/computer-vision/#overview)
- Firebase Storage / Authentication / Firestore
- Stripe
- Google Play Services

Binaries shipped on Google Play Store will not contain stripe dependencies. Binaries shipped on my personal F-Droid will contain Stripe dependencies. 
 
I am giving away this software for free in hope that someone will find it useful. You can self host it on your personal server. If you are using my hosted service,
it will cost a small fee (it's a subscription service). This is my first time releasing a server / client application to the world, I welcome any feedback you may have! 


#### Running

As this is a MVP, it will take a considerable amount of time to setup and get this running. I will reiterate and improve on it in future releases. 


You will need a couple of environment variable to get this to run. 

```
AZUREKEY="xxx"
AZUREURL="xxx"
STRIPE_SECRET_KEY="xxx"
STORAGE_BUCKET="xxx"
STRIPE_WEB_HOOK_KEY="xxx"
```

I have included a systemd service in `conf` folder. 


You will need to get your personal `GeoLite2-Country.mmdb` from [maxmind](https://dev.maxmind.com/geoip/geolite2-free-geolocation-data?lang=en). Place the file
in `server/src/main/resources/GeoLite2-Country.mmdb`. 

You will need `credentials.json` (service account on google cloud) and `play_publisher_credentials.json` (service account on google play) and place them in 
`server/src/main/resources/credentials/`

You will need to create `swot` in `server/src/main/resources/` and place the files taken from [Jetbrain](https://github.com/JetBrains/swot/)



### License

Both server and client code are licensed under a strong copyleft license. 

Client code is licensed under GPLv3
```
	Copyright (c) 2021 ASDF Dev Pte. Ltd.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
```


Server code is licensed under AGPLv3

```
	Copyright (c)  2021 ASDF Dev Pte. Ltd.
	
	This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
```
