Ext.namespace('Hippo.Util');

Hippo.Util.ParseUrlForIE = function(url) {
    if (Ext.isIE && (Ext.isIE8 || Ext.isIE7)) {
        var key = '&_hstForceUniqueUrl=';
        var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz";
        var string_length = 16;

        if(url.indexOf('?') == -1) {
            url += '?'
        }
        var keyIndex = url.indexOf(key);
        if(keyIndex > -1) {
            var tmpUrl = url.substring(0, keyIndex);
            tmpUrl += url.substring(keyIndex + key.length + string_length);
            url = tmpUrl;
        }
        url += key;

        for (var i=0; i<string_length; i++) {
            var rnum = Math.floor(Math.random() * chars.length);
            url += chars.substring(rnum,rnum+1);
        }
    }
    return url;
};
