if (typeof Hippo === 'undefined') {
    Hippo = {};
}
if (typeof Hippo.Hst === 'undefined') {
    Hippo.Hst = {};
}

Hippo.Hst.AsyncPage = {

    load : function() {
        var result,divs , i, length;
        result = [];

        if (document.getElementsByClassName) {
            result = document.getElementsByClassName('_async');
        } else {
            divs = document.getElementsByTagName('div');
            for (i=0, length=divs.length; i<length; i++) {
                if (divs[i].className === '_async') {
                    result.push(divs[i]);
                }
            }
        }

        for (i=0, length=result.length; i< length; i++) {
            (function(element) {
                this.sendRequest(element.id, function(xmlHttp) {
                    var fragment, tmpDiv, parent;
                    fragment = document.createDocumentFragment();
                    tmpDiv = document.createElement('div');
                    tmpDiv.innerHTML = xmlHttp.responseText;
                    while (tmpDiv.firstChild) {
                        fragment.appendChild(tmpDiv.firstChild);
                    }

                    parent = element.parentNode;

                    var scriptNodes = [];
                    var scripts = fragment.querySelectorAll('script');
                    for (var i=0, length=scripts.length; i<length; i++) {
                        var script = document.createElement('script');
                        var scriptText = document.createTextNode(scripts[i].innerHTML);
                        script.appendChild(scriptText);

                        scripts[i].parentNode.removeChild(scripts[i]);
                        scriptNodes.push(script);
                    }

                    var reference;
                    if (element.nextSibling) {
                        reference = element.nextSibling;
                    } else {
                        reference = parent;
                    }

                    parent.replaceChild(fragment, element);

                    for (var i=0, length=scriptNodes.length; i<length; i++) {
                        if (parent !== reference) {
                            parent.insertBefore(scriptNodes[i], reference);
                        } else {
                            parent.appendChild(scriptNodes[i]);
                        }
                    }
                });
            }).call(this, result[i]);
        }
    },

    sendRequest : function(url, callback) {
        var xmlHttpRequest;
        try {
            xmlHttpRequest = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
            xmlHttpRequest.open("GET", url, true);
            xmlHttpRequest.onreadystatechange = function () {
                if (xmlHttpRequest.readyState !== 4) {
                    return;
                }
                if (xmlHttpRequest.status !== 200 && xmlHttpRequest.status !== 304) {
                    return;
                }
                callback(xmlHttpRequest);
            };

            xmlHttpRequest.send();
        } catch (e) {
            if (typeof window.console !== 'undefined') {
                if (typeof console.error !== 'undefined') {
                    console.error(e.name + ": " + e.message);
                } else if (typeof console.log !== 'undefined') {
                    console.log(e.name + ": " + e.message);
                }
            }
        }
    }
};