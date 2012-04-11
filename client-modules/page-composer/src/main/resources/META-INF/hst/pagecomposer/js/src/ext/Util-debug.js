Ext.namespace('Hippo.Util');

Hippo.Util.ParseUrlForIE = function(url) {
    if (!Ext.isIE7 && !Ext.isIE8) {
        return url;
    }

    var newUrl = '';
    var generateRandomString = function(stringLength) {
        var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz";
        var string = '';
        for (var i=0; i<stringLength; i++) {
            var rnum = Math.floor(Math.random() * chars.length);
            string += chars.substring(rnum,rnum+1);
        }
        return string;
    };

    var key = '_hstForceUniqueUrl=';
    var randomString = generateRandomString(16);
    if (url.indexOf(key) !== -1) {
        newUrl = url.replace(/([&?]{1})_hstForceUniqueUrl=[0-9A-Za-z]{16}/g, '$1_hstForceUniqueUrl='+randomString);
    } else if (url.indexOf('?') === -1) {
        newUrl = url + '?' + key + randomString;
    } else {
        newUrl = url + '&' + key + randomString;
    }

    return newUrl;
};