/*
 *  Copyright 2010 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

jQuery.noConflict();
(function($) {

    $.namespace('Hippo.Util');

    Hippo.Util.Map = Class.extend({
        init : function() {
            this.keys = [];
            this.values = {};
        },

        put : function(key, value) {
            this.keys.push(key);
            this.values[key] = value;
        },

        get : function(key) {
            if (this.containsKey(key)) {
                return this.values[key];
            }
            return null;
        },

        containsKey : function(key) {
            return $.inArray(key, this.keys) > -1;
        },

        remove : function(key) {
            var idx = $.inArray(key, this.keys);
            if (idx > -1) {
                this.keys.removeByIndex(idx);
                var v = this.values[key];
                delete this.values[key];
                return v;
            } else {
                throw new Error('Remove failed: No entry found for key ' + key)
            }
        },

        each: function(f, scope) {
            scope = scope || this;
            var len = this.keys.length;
            for (var i = 0; i < len; ++i) {
                var key = this.keys[i];
                f.apply(scope, [key, this.values[key]]);
            }
        },

        keySet : function() {
            var keys = [], len = this.keys.length;
            for(var i=0; i<len; ++i) {
                keys.push(this.keys[i]);
            }
            return keys;
        },

        getIndexMap : function() {
            var map = {};
            for(var i=0; i<this.keys.length; i++) {
                map[this.keys[i]] = i+1;
            }
            return map;
        },

        size : function() {
            return this.keys.length;
        },

        clear : function() {
            this.keys = [];
            this.values = {};
        }

    });

    Hippo.Util.OrderedMap = Hippo.Util.Map.extend({
        put : function(key, value, index) {
            if (typeof index === 'undefined') {
                this._super(key, value);
            } else {
                this.keys.splice(index, 0, key);
                this.values[key] = value;
            }
        },

        updateOrder : function (order) {
            var old = this.keys;
            this.keys = order;
            return order != old;
        }
    });

    Hippo.Util.Draw = Class.extend({
        init: function(cfg) {
            this.cache = {};
            this.config = $.extend({
                min             : 0,
                thresholdHigh   : 0.8,
                thresholdLow    : 0,
                beforeOffset    : 2,
                afterOffset     : 2
            }, cfg);
        },

        _draw : function(type, source, el, pos, direction, opts) {
            opts = $.extend({}, this.config, opts);

            var key = type + source[0].id;
            var o = this.cache[key];
            if(typeof o === 'undefined') {
                var src = {
                    offset : source.offset(),
                    width  : source.outerWidth(),
                    height : source.outerHeight()
                };
                var ind = {
                    direction: direction,
                    width  : src.width,
                    height : src.height,
                    left: src.offset.left,
                    top: src.offset.top
                };

                if(direction == HST.DIR.VERTICAL) {
                    ind.width -= (ind.width  * opts.thresholdLow);
                    ind.height = opts.min > 0 ? opts.min : ind.height - (ind.height * opts.thresholdHigh);
                } else if(direction == HST.DIR.HORIZONTAL) {
                    ind.height -= (ind.height * opts.thresholdLow);
                    ind.width = opts.min > 0 ? opts.min : ind.width - (ind.width * opts.thresholdHigh);
                }
                o = {src : src, ind : ind};
                pos.call(this, o);
                this.cache[key] = o;
            }

            el.width(o.ind.width);
            el.height(o.ind.height);
            el.offset({
                left: o.ind.left,
                top : o.ind.top
            });
        },

        inside : function (source, el, direction, opts) {
            this._draw('inside', source, el, function(data) {
                data.ind.left += (data.src.width-data.ind.width) / 2;
                data.ind.top  += (data.src.height-data.ind.height) / 2;
            }, direction, opts);
        },

        after : function(source, el, direction, opts) {
            this._draw('after', source, el, function(data) {
                if(direction == HST.DIR.VERTICAL) {
                    data.ind.left += (data.src.width-data.ind.width) / 2;
                    data.ind.top  += data.src.height + this.config.afterOffset;
                } else {
                    data.ind.left += data.src.width + this.config.afterOffset;
                    data.ind.top -= this.config.afterOffset;
                }
            }, direction, opts);
        },

        before : function(source, el, direction, opts) {
            this._draw('before', source, el, function(data) {
                if(direction == HST.DIR.VERTICAL) {
                    data.ind.left += (data.src.width - data.ind.width) / 2;
                    data.ind.top -= this.config.beforeOffset;
                } else if(direction == HST.DIR.HORIZONTAL) {
                    data.ind.left -= this.config.beforeOffset;
                    data.ind.top -= this.config.beforeOffset;
                }
            }, direction, opts);
        },

        between : function(prev, next, el, direction, opts) {
            //take prev as source
            this._draw('between', prev, el, function(data) {

                var nextPosition = next.offset();
                if(direction == HST.DIR.VERTICAL) {
                    data.ind.left += (data.src.width - data.ind.width) / 2;
                    var bottom = data.ind.top + data.src.height;
                    data.ind.top = bottom + ((nextPosition.top - bottom) / 2) - (data.ind.height/2);
                } else if(direction == HST.DIR.HORIZONTAL) {
                    data.ind.left += data.src.width;
                }

            }, direction, opts);
        },

        reset : function() {
            this.cache = {};
        }
    });

    Hippo.Util.getElementPath = function(element) {
        var path = "";
        var nodeString = "";
        var node = element;
        while (node.parentNode != null) {
            nodeString = node.tagName;
            if (node.getAttribute('id')) {
                nodeString += "[id="+node.getAttribute('id')+"]";
            } else if (node.className) {
                nodeString += "[class="+node.className+"]";
            }
            if (path.length > 0) {
                path = " > " + path;
            }
            path = nodeString + path;
            node = node.parentNode;
        }
        return path;
    };

})(jQuery);


//Copied from http://ejohn.org/blog/javascript-array-remove/
//Adds removeByIndex to array
Array.prototype.removeByIndex= function(from, to) {
    var rest = this.slice((to || from) + 1 || this.length);
    this.length = from < 0 ? this.length + from : from;
    return this.push.apply(this, rest);
};
