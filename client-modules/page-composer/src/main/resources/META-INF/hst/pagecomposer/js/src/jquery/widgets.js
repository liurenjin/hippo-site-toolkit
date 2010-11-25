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

    $.namespace('Hippo.PageComposer.UI', 'Hippo.PageComposer.UI.Container', 'Hippo.PageComposer.UI.ContainerItem');

    Hippo.PageComposer.UI.Widget = Class.extend({
        init : function(id, element) {
            this.id = id;
            this.overlayId = id + '-overlay';
            this.element = element;
            this.el = $(element);

            this.parent = null;

            this.noHover = false;

            //selector shortcuts
            this.sel = {
                self : '#' + element.id,
                overlay : '#' + this.overlayId
            };

            //class values
            this.cls = {
                selected: 'hst-selected',
                activated: 'hst-activated',
                overlay : {
                    base: 'hst-overlay',
                    hover : 'hst-overlay-hover',
                    mark : null,
                    custom : null
                }
            };

            this.rendered = false;
        },

        select : function() {
            $(this.element).addClass(this.cls.selected);
            $(this.overlay).addClass(this.cls.selected);
        },

        deselect : function() {
            $(this.element).removeClass(this.cls.selected);
            $(this.overlay).removeClass(this.cls.selected);
        },

        activate : function() {
            $(this.element).addClass(this.cls.activated);
        },

        deactivate : function() {
            $(this.element).addClass(this.cls.activated);
        },

        render : function(parent) {
            if (this.rendered) {
                return;
            }
            this.parent = parent;

            var parentOverlay = $.isFunction(parent.getOverlay) ? parent.getOverlay() : document.body;
            var overlay = $('<div/>').addClass(this.cls.overlay.base).appendTo(parentOverlay);
            if(this.cls.overlay.mark != null) {
                overlay.addClass(this.cls.overlay.mark);
            }
            if(this.cls.overlay.custom != null) {
                overlay.addClass(this.cls.overlay.custom);
            }
            overlay.css('position', 'absolute');
            overlay.attr('hst:id', this.id);
            overlay.attr('id', this.overlayId);

            var self = this;
            overlay.hover(function() {
                self.onMouseOver(this);
            }, function() {
                self.onMouseOut(this);
            });
            overlay.click(function() {
                self.onClick();
            });
            this.overlay = overlay;
            this._syncOverlay();

            this.onRender();

            this.rendered = true;
        },

        updateSharedData: function(facade) {
            this.items.each(function(key, item) {
                item.updateSharedData(facade);
            });
        },

        toggleNoHover : function() {
            this.noHover = !this.noHover;
        },

        sync: function() {
            this._syncOverlay();
        },

        destroy : function() {
            this.onDestroy();
            this.rendered = false;
        },

        getOverlay : function() {
            return this.overlay;
        },

        onClick : function() {
        },

        onMouseOver : function(element) {
            if (!this.noHover) {
                this.getOverlay().addClass(this.cls.overlay.hover);
            }
        },

        onMouseOut : function(element) {
            if (!this.noHover) {
                this.getOverlay().removeClass(this.cls.overlay.hover);
            }
        },

        onRender  : function() {
        },

        onDestroy : function() {
        },

        _syncOverlay : function() {
            var el = this.getOverlaySource();
            var elOffset = el.offset();
            var overlay = this.overlay;

            //test for single border and assume it all around.
            //TODO: test all borders
            var border = overlay.css('border-left-width');
            var borderWidth = !!border ? parseFloat(border.substring(0, border.length - 2)) : 0;

            var data = {
                left: elOffset.left,
                top: elOffset.top,
                width : el.outerWidth(),
                height: el.outerHeight(),
                position: 'absolute',
                overlayBorder: borderWidth
            };

            data = this.getOverlayData(data);
            this.overlay.
                    css('left', data.left).
                    css('top', data.top).
                    css('position', data.position).
                    width(data.width).
                    height(data.height);

            this._cachedOverlayData = data;
            this.onSyncOverlay();
        },

        getOverlayData : function(data) {
        },

        getOverlaySource : function() {
            return $(this.element);
        },

        onSyncOverlay : function() {
        }
    });

    Hippo.PageComposer.UI.Container.Base = Hippo.PageComposer.UI.Widget.extend({
        init : function(id, element) {
            this._super(id, element);

            this.state = new Hippo.PageComposer.UI.DDState();
            this.items = new Hippo.Util.OrderedMap();
            this.dropIndicator = null;
            this.draw = new Hippo.Util.Draw({min: 3, tresholdLow: 0});

            this.parentMargin = 0; //margin of overlay

            this.cls.selected       = this.cls.selected + '-container';
            this.cls.activated      = this.cls.activated + '-container';
            this.cls.highlight      = 'hst-highlight';
            this.cls.overlay.custom = 'hst-overlay-container';

            this.cls.container      = 'hst-container';
            this.cls.item           = 'hst-container-item';
            this.cls.emptyContainer = 'hst-empty-container';
            this.cls.emptyItem      = 'hst-empty-container-item';
            this.cls.overlay.item   = 'hst-overlay-container-item';

            this.sel.container      = this.sel.self + ' .' + this.cls.container;
            this.sel.item           = this.sel.self + ' div.componentContentWrapper';
            this.sel.itemWrapper    = this.sel.self + ' .' + this.cls.item;

            this.sel.sortable       = this.sel.overlay;
            this.sel.sort = {
                items : this.sel.sortable + ' .' + this.cls.overlay.item,
                itemsRel : '.' + this.cls.overlay.item
            };

            this.sel.append = {
                item    : '',
                container : '',
                insertAt : ''
            };

            //workaround: set to opposite to evoke this.sync() to render an initially correct UI
            this.isEmpty = $(this.sel.item).size() > 0;

            var self = this;
            $(this.sel.item).each(function() {
                self._insertNewItem(this, true);
            });
        },

        onRender : function() {
            this._super();
            this._renderItems();
            this._createSortable();
            this._checkEmpty();
            this.sync();
        },

        onDestroy: function() {
            this._destroySortable();
        },

        _renderItems : function() {
            this.eachItem(function(key, item) {
                this._renderItem(item);
            });
        },

        _renderItem : function(item) {
            item.render(this);
        },

        _insertNewItem : function(element, exists, index) {
            var item = Hippo.PageComposer.UI.Factory.createOrRetrieve(element);
            this.items.put(item.id, item, index);
            if(!exists) {
                var itemElement = this.createItemElement(item.element);
                this.appendItem(itemElement, index);
            }
            return item;
        },

        _syncAll : function() {
            this.parent.checkStateChanges();
        },

        _updateOrder : function(itemsUpToDate) {
            var order = [], lookup = {}, count = 0;
            if(itemsUpToDate) {
                //items in container are up to date, no re-order needed
                order = this.items.keySet();
            } else {
                //replicate order from layout-items
                //then sort container-item elements according to new order
                $(this.sel.sort.items).each(function() {
                    var id = $(this).attr(HST.ATTR.ID);
                    order.push(id);
                    lookup[id] = count++;
                });
                this.items.updateOrder(order);
                var container = $(this.sel.container);
                var items = $(this.sel.itemWrapper).get();
                items.sort(function(a, b) {
                    a = lookup[$('div.componentContentWrapper', a).attr(HST.ATTR.ID)];
                    b = lookup[$('div.componentContentWrapper', b).attr(HST.ATTR.ID)];
                    return (a < b) ? -1 : (a > b) ? 1 : 0;
                });
                $.each(items, function(idx, itm) {
                    container.append(itm);
                });
            }
            sendMessage({id: this.id, children: order}, 'rearrange');
        },

        //if container is empty, make sure it still has a size so items form a different container can be dropped
        _checkEmpty : function() {
            if(this.items.size() == 0) {
                if(!this.el.hasClass(this.cls.emptyContainer)) {
                    this.el.addClass(this.cls.emptyContainer);
                    this.overlay.addClass(this.cls.emptyContainer);
                    var tmpCls = this.cls.item;
                    this.cls.item = this.cls.emptyItem;
                    var item = this.createItemElement($('<div class="empty-container-placeholder">Empty container</div>')[0]);
                    this.appendItem(item);
                    this.cls.item = tmpCls;
                }
            } else if(this.el.hasClass(this.cls.emptyContainer)) {
                this.el.removeClass(this.cls.emptyContainer);
                this.overlay.removeClass(this.cls.emptyContainer);
                $(this.sel.container + ' .' + this.cls.emptyItem).remove();
            }
        },

        _syncItems : function(quite) {
            this.eachItem(function(key, item) {
                if (typeof item !== 'undefined') {
                    item.sync();
                } else if(!quite && Hippo.PageComposer.Main.isDebug()) {
                    console.warn('ContainerItem with id=' + id + ' is not found in active map.');
                }
            });
        },

        _createSortable : function() {
            //instantiate jquery.UI sortable
            $(this.sel.sortable).sortable({
                //revert: 100,
                items: this.sel.sort.itemsRel,
                connectWith: '.' + this.cls.overlay.base,
                start   : $.proxy(this.ddOnStart, this),
                stop    : $.proxy(this.ddOnStop, this),
                helper  : $.proxy(this.ddHelper, this),
                update  : $.proxy(this.ddOnUpdate, this),
                receive : $.proxy(this.ddOnReceive, this),
                remove  : $.proxy(this.ddOnRemove, this),
                tolerance : 'pointer',
                change : $.proxy(this.ddOnChange, this)
            }).disableSelection();
        },

        _destroySortable : function() {
            $(this.sel.sortable).sortable('destroy');
        },

        beforeDrag : function() {
            this.toggleNoHover();
        },

        afterDrag : function() {
            this.toggleNoHover();
            this.draw.reset();
        },

        ddOnStart : function(event, ui) {
            var id = $(ui.item).attr(HST.ATTR.ID);
            var item = this.items.get(id);
            this.parent.onDragStart(ui, this);
            item.onDragStart(event, ui);
        },

        ddOnStop: function(event, ui) {
            var id = $(ui.item).attr(HST.ATTR.ID);
            if(this.items.containsKey(id)) {
                var item = this.items.get(id);
                item.onDragStop(event, ui);
            }
            this.parent.onDragStop(ui);
            this._syncAll();
        },

        ddOnUpdate : function(event, ui) {
            this.state.orderChanged = true;
        },

        /**
         * ddOnReceive is not called in the onStart-onStop lifecycle, but independently, so it can not depend on this.state
         * but calls stateChecking itself
         */
        ddOnReceive : function(event, ui) {
            var id = ui.item.attr(HST.ATTR.ID);
            var item = Hippo.PageComposer.UI.Factory.getById(id);
            var self = this;
            $(this.sel.sort.items).each(function(index) {
                var itemId = $(this).attr(HST.ATTR.ID);
                if(itemId == id) {
                    item.onDragStop(event, ui);
                    item.destroy();
                    self.add(item.element, index);
                    self.state.itemsUpToDate = true;
                    return false;
                }
            });
        },

        ddOnRemove : function(event, ui) {
            var id = $(ui.item).attr(HST.ATTR.ID);
            this.removeItem(id, true);
        },

        ddHelper : function(event, element) {
            var id = element.attr(HST.ATTR.ID);
            var item = this.items.get(id);
            return item.menu.clone().css('width', '85px').css('height', '18px').offset({top: event.clientY, left:event.clientX}).appendTo(document.body);
//            return $('<div class="hst-dd-helper">' + item.data.name + '</div>').css('width', '85px').css('height', '18px').offset({top: event.clientY, left:event.clientX}).appendTo(document.body);
        },

        ddOnChange : function(event, ui) {
            this.parent.onDrag(ui, this);
        },

        drawDropIndicator : function(ui, el) {
            if(ui.placeholder.siblings().length == 0) {
                //draw indicator inside empty container
                this.draw.inside(this.el, el);
            } else {
                var prev = ui.placeholder.prev();
                var next = ui.placeholder.next();
                var getEl = function(_el) {
                    return Hippo.PageComposer.UI.Factory.getById(_el.attr(HST.ATTR.ID)).el;
                };
                var original = ui.item[0];
                if(prev[0] == original || (next.length > 0 && next[0] == original)) {
                    this.draw.inside(getEl(ui.item), el);
                } else {
                    if(prev.length == 0) {
                        this.draw.before(getEl(next), el);
                    } else if (next.length == 0) {
                        this.draw.after(getEl(prev), el);
                    } else {
                        this.draw.between(getEl(prev), getEl(next), el);
                    }
                }
            }
        },

        getOverlayData : function(data) {
            if(data.overlayBorder > 0) {
                var total = data.overlayBorder * 2;
                data.left -= total;
                data.top -= total;
                data.width += total;
                data.height += total;

                this.parentMargin = data.overlayBorder;
            }
            return data;
        },

        highlight : function() {
            this.overlay.addClass(this.cls.highlight);
        },

        unhighlight : function() {
            this.overlay.removeClass(this.cls.highlight);
        },

        checkState : function() {
            if(this.state.checkEmpty) {
                this._checkEmpty();
            }
            if(this.state.orderChanged) {
                this._updateOrder(this.state.itemsUpToDate);
                this.parent.requestSync();
            }
            this.state.reset();
        },

        toggleNoHover : function() {
            this._super();
            this.eachItem(function(k, item) {
                item.toggleNoHover();
            });
        },


        sync : function() {
            this._syncOverlay();
            this._syncItems(true);
        },

        add : function(element, index) {
            var item = this._insertNewItem(element, false, index);
            this._renderItem(item);
            $(this.sel.sortable).sortable('refresh');

            this.state.checkEmpty = true;
            this.state.orderChanged = true;
            this.state.itemsUpToDate = true;
        },

        remove: function(id) {
        },

        removeItem : function(id, quite) {
            if(this.items.containsKey(id)) {
                var item = this.items.remove(id);
                //remove item wrapper elements
                $(item.element).parents('.' + this.cls.item).remove();
                if(!quite) {
                    item.destroy();
                }

                this.state.checkEmpty = true;
                this.state.orderChanged = true;
                return true;
            }
            return false;
        },

        hasItem : function(id) {
            return this.items.containsKey(id);
        },

        //Param f is a function with signature function(key, value)
        eachItem : function(f, scope) {
            this.items.each(f, scope || this);
        },

        appendItem : function(item, index) {
            if ($(this.sel.append.item).size() == 0) {
                $(this.sel.append.container).append(item);
            } else {
                if(index > -1) {
                    if(index == 0) {
                        item.insertBefore(this.sel.append.insertAt + ':eq(0)');
                    } else {
                        item.insertAfter(this.sel.append.insertAt + ':eq(' + (index-1) + ')');
                    }
                } else {
                    item.insertAfter(this.sel.append.insertAt + ':last');
                }
            }

        },

        /**
         * Template method for wrapping a containerItem element in a new item element
         */
        createItemElement : function(element) {
        }

    });
    Hippo.PageComposer.UI.Factory.register('HST.BaseContainer', Hippo.PageComposer.UI.Container.Base);

    //Container implementations
    Hippo.PageComposer.UI.Container.Table = Hippo.PageComposer.UI.Container.Base.extend({

        init : function(id, element) {
            this._super(id, element);

            this.sel.append.item = this.sel.container + ' > tbody > tr.' + this.cls.item;
            this.sel.append.container = this.sel.container + ' > tbody';
            this.sel.append.insertAt = this.sel.container + ' > tbody > tr';
        },

        createItemElement : function(element) {
            var td = $('<td></td>').append(element);
            return $('<tr class="' + this.cls.item + '"></tr>').append(td);
        }

    });
    Hippo.PageComposer.UI.Factory.register('HST.Table', Hippo.PageComposer.UI.Container.Table);

    Hippo.PageComposer.UI.Container.UnorderedList = Hippo.PageComposer.UI.Container.Base.extend({

        init : function(id, element) {
            this._super(id, element);

            this.sel.append.item = this.sel.container + ' > li.' + this.cls.item;
            this.sel.append.container = this.sel.container;
            this.sel.append.insertAt = this.sel.container + ' > li';
        },

        createItemElement : function(element) {
            return $('<li class="' + this.cls.item + '"></li>').append(element);
        }

    });
    Hippo.PageComposer.UI.Factory.register('HST.UnorderedList', Hippo.PageComposer.UI.Container.UnorderedList);

    Hippo.PageComposer.UI.Container.OrderedList = Hippo.PageComposer.UI.Container.Base.extend({

        init : function(id, element) {
            this._super(id, element);

            this.sel.append.item = this.sel.container + ' > li.' + this.cls.item;
            this.sel.append.container = this.sel.container;
            this.sel.append.insertAt = this.sel.container + ' > li';
        },

        createItemElement : function(element) {
            return $('<li class="' + this.cls.item + '"></li>').append(element);
        }

    });
    Hippo.PageComposer.UI.Factory.register('HST.OrderedList', Hippo.PageComposer.UI.Container.OrderedList);

    Hippo.PageComposer.UI.ContainerItem.Base = Hippo.PageComposer.UI.Widget.extend({
        init : function(id, element) {
            this._super(id, element);

            this.cls.selected = this.cls.selected + '-containerItem';
            this.cls.activated = this.cls.activated + '-containerItem';
            this.cls.overlay.mark = 'hst-overlay-container-item';

            var el = $(element);
            var tmp = el.attr("hst:temporary");
            this.isTemporary = typeof tmp !== 'undefined';
            if(this.isTemporary) {
                el.html('Click to refresh');
            }

            this.data = {
                name : 'loading..'
            };
        },

        onRender : function() {
            var background = $('<div/>').addClass('hst-overlay-background');
            this.overlay.append(background);

            this.menu = $('<div/>').addClass('hst-overlay-menu').appendTo(document.body);

            var data = {element: this.element};
            var deleteButton = $('<div/>').addClass('hst-overlay-menu-button').html('X');
            deleteButton.click(function(e) {
                e.stopPropagation();
                sendMessage(data, 'remove');
            });
            this.menu.append(deleteButton);

            var nameLabel = $('<div/>').addClass('hst-overlay-name-label');
            this.menu.append(nameLabel);
            this.nameLabel = nameLabel;

            this.renderLabelContents();
        },

        sync: function() {
            this._super();
            this.menu.position({
                my : 'right top',
                at : 'right top',
                of : this.overlay,
                offset : '-2 2'
            });

        },

        getOverlayData : function(data) {
            data.position = 'inherit';
            var parentOffset = this.parent.overlay.offset();
            data.left -= (parentOffset.left + this.parent.parentMargin);
            data.top -= (parentOffset.top + this.parent.parentMargin);
            data.width  -= data.overlayBorder*2;
            data.height -= data.overlayBorder*2;

            return data;
        },

        updateSharedData : function(facade) {
            this.data.name = facade.getName(this.id);

            this.renderLabelContents();
        },

        renderLabelContents : function() {
            this.nameLabel.html(this.data.name);
        },

        getOverlaySource : function() {
            return $(this.element);
    //        return $(this.element).parents('.hst-container-item')
        },

        onClick : function() {
            if(this.isTemporary) {
                sendMessage({}, 'refresh');
            } else {
                sendMessage({element: this.element}, 'onclick');
            }
        },

        onDragStart : function(event, ui) {
            $(this.element).addClass('hst-item-ondrag');
            this.menu.hide();
        },

        onDragStop : function(event, ui) {
            $(this.element).removeClass('hst-item-ondrag');
            this.menu.show();
        },

        onDestroy : function() {
            this.overlay.remove();
            this.menu.remove();
        }

    });
    Hippo.PageComposer.UI.Factory.register('HST.Item', Hippo.PageComposer.UI.ContainerItem.Base);


    Hippo.PageComposer.UI.DDState = function() {
        this.orderChanged = false;
        this.checkEmpty = false;
        this.itemsUpToDate = false;
    };

    Hippo.PageComposer.UI.DDState.prototype = {
        reset : function() {
            this.orderChanged = false;
            this.checkEmpty = false;
            this.itemsUpToDate = false;
        }
    };

})(jQuery);