Ext.namespace("Hippo.App");Hippo.App.PageEditor=Ext.extend(Ext.App,{loadMessage:"Initializing Template Composer ... ",init:function(){if(this.debug){Ext.Ajax.timeout=90000}this.ids={page:null,toolkit:null,site:null};this.stores={toolkit:null,pageModel:null};this.pageModelFacade=null;this.initUI()},keepAlive:function(){if(this.ids.site!=null){Ext.Ajax.request({url:"_rp/"+this.ids.site+"./keepalive",success:function(){}})}},initUI:function(){var a=new Ext.Viewport({layout:"fit",title:"Hippo PageEditor",renderTo:Ext.getBody(),items:[{id:"Iframe",xtype:"iframepanel",defaultSrc:this.iframeUrl,collapsible:false,disableMessaging:false,tbar:[{text:"Template Composer",iconCls:"title-button",id:"pageComposerButton",enableToggle:true,pressed:true,width:150,listeners:{click:{fn:this.toggleConfigWindow,scope:this}}},"->",{text:"Logout",id:"logoutButton",listeners:{click:{fn:this.doLogout,scope:this}}}],listeners:{message:{fn:this.handleFrameMessages,scope:this},documentloaded:{fn:function(b){if(Ext.isSafari||Ext.isChrome){this.onIframeDOMReady(b)}},scope:this},domready:{fn:function(b){if(Ext.isGecko||Ext.isIE){this.onIframeDOMReady(b)}},scope:this},exception:{fn:function(b,c){console.error(c)},scope:this},resize:{fn:function(){this.sendFrameMessage({},"resize")},scope:this}}}]})},toggleConfigWindow:function(){var a=Ext.getCmp("Iframe");a.sendMessage({},"toggle");if(!this.mainWindow.isVisible()){this.mainWindow.show("pageComposerButton")}else{this.mainWindow.hide("pageComposerButton")}},refreshIframe:function(){Ext.Msg.wait("Reloading page ...");var a=Ext.getCmp("Iframe");a.getFrameDocument().location.reload(true)},doLogout:function(){Ext.Ajax.request({url:"_rp/"+this.ids.site+"./logout",success:function(){location.reload(true)}})},onIframeDOMReady:function(a){a.execScript("Hippo.PageComposer.Main.init("+this.debug+")",true);Ext.Msg.hide()},onIframeAppLoaded:function(d){var f=d.siteIdentifier;var c=d.toolkitIdentifier;var a=d.rootComponentIdentifier;if(c!=this.ids.toolkit){this.stores.toolkit=this.createToolkitStore(c);this.stores.toolkit.load()}if(a!=this.ids.page){this.stores.pageModel=this.createPageModelStore(a);this.stores.pageModel.load()}else{this.shareData()}if(!this.mainWindow){var e=this.mainWindow=this.createMainWindow();this.beforeQuit(e.close,e);e.show()}else{if(c!=this.ids.toolkit){var b=Ext.getCmp("ToolkitGrid");b.reconfigure(this.stores.toolkit,b.getColumnModel())}if(a!=this.ids.page){var b=Ext.getCmp("PageModelGrid");b.reconfigure(this.stores.pageModel,b.getColumnModel())}}this.ids.page=a;this.ids.toolkit=c;this.ids.site=f;Ext.TaskMgr.start({run:this.keepAlive,interval:60000,scope:this})},createToolkitStore:function(a){return new Hippo.App.ToolkitStore({toolkitId:a})},createPageModelStore:function(a){return new Hippo.App.PageModelStore({rootComponentIdentifier:this.rootComponentIdentifier,pageId:a,listeners:{write:{fn:function(l,f,m,k,d){if(f=="create"){d=Ext.isArray(d)?d:[d];for(var g=0;g<d.length;g++){var j=d[g];if(j.get("type")==HST.CONTAINERITEM){this.sendFrameMessage({parentId:j.get("parentId"),element:j.get("element")},"add");var h=j.get("parentId");var e=l.findExact("id",h);var b=l.getAt(e);var c=b.get("children");c.push(j.get("id"));b.set("children",c)}}}else{if(f=="update"){if(!this.isReloading){l.reload();this.isReloading=true}}}},scope:this},load:{fn:function(c,b,d){this.isReloading=false;this.shareData()},scope:this},remove:{fn:function(c,b,d){if(b.get("type")==HST.CONTAINER){Ext.each(b.get("children"),function(i){var h=c.findExact("id",i);if(h>-1){c.removeAt(h)}})}else{var g=c.getAt(c.findExact("id",b.get("parentId")));if(typeof g!=="undefined"){var f=g.get("children");f.remove(b.get("id"));g.set("children",f)}}var e=Ext.getCmp("PageModelGrid");if(e.getSelectionModel().getSelected()==b){this.deselect(null,null,b)}this.sendFrameMessage({element:b.data.element},"remove")},scope:this}}})},createMainWindow:function(){var a=new Hippo.ux.window.FloatingWindow({title:"Configuration",x:10,y:35,width:310,height:650,initRegion:"right",layout:"border",closable:true,constrainHeader:true,closeAction:"hide",bodyStyle:"background-color: #ffffff",items:[{region:"north",split:true,layout:"accordion",height:300,items:[{xtype:"h_base_grid",flex:2,id:"ToolkitGrid",title:"Toolkit",store:this.stores.toolkit,cm:new Ext.grid.ColumnModel({columns:[{header:"Name",dataIndex:"name",id:"name",viewConfig:{width:40}}],defaults:{sortable:true,menuDisabled:true}}),plugins:[Hippo.App.DragDropOne]},{xtype:"h_base_grid",flex:3,id:"PageModelGrid",title:"Containers",store:this.stores.pageModel,sm:new Ext.grid.RowSelectionModel({singleSelect:true,listeners:{rowselect:{fn:this.select,scope:this},rowdeselect:{fn:this.deselect,scope:this}}}),cm:new Ext.grid.ColumnModel({columns:[{header:"Name",dataIndex:"name",id:"name",viewConfig:{width:120}},{header:"Type",dataIndex:"type",id:"type"},{header:"Template",dataIndex:"template",id:"template"}],defaults:{sortable:false,menuDisabled:true}}),menuProvider:this}]},{id:"componentPropertiesPanel",xtype:"h_properties_panel",region:"center",split:true}]});return a},shareData:function(){var a=this;var b=function(){};b.prototype={getName:function(e){var c=a.stores.pageModel.findExact("id",e);if(c==-1){return null}var d=a.stores.pageModel.getAt(c);return d.get("name")}};if(this.pageModelFacade==null){this.pageModelFacade=new b()}this.sendFrameMessage(this.pageModelFacade,"sharedata")},handleOnClick:function(b){var d=b.getAttribute("hst:id");var a=this.stores.pageModel.findExact("id",d);if(a<0){console.warn("Handling onClick for element[@hst:id="+d+"] with no record in component store");return}var c=Ext.getCmp("PageModelGrid").getSelectionModel();if(c.isSelected(a)){c.deselectRow(a)}else{c.selectRow(a)}},findElement:function(c){var b=Ext.getCmp("Iframe").getFrameDocument();var a=b.getElementById(c);return a},select:function(c,b,a){this.sendFrameMessage({element:a.data.element},"select");if(a.get("type")===HST.CONTAINERITEM){this.showProperties(a)}},deselect:function(c,b,a){this.sendFrameMessage({element:a.data.element},"deselect");this.hideProperties()},onRearrangeContainer:function(d,c){var b=this.stores.pageModel.findExact("id",d);var a=this.stores.pageModel.getAt(b);a.set("children",c);a.commit()},handleReceivedItem:function(a,b){},sendFrameMessage:function(b,a){Ext.getCmp("Iframe").getFrame().sendMessage(b,a)},showProperties:function(a){Ext.getCmp("componentPropertiesPanel").reload(this.ids.site,a.get("id"),a.get("name"),a.get("path"))},hideProperties:function(){Ext.getCmp("componentPropertiesPanel").removeAll()},getMenuActions:function(a,e){var f=[];var b=this.stores.pageModel;var d=a.get("type");if(d==HST.CONTAINERITEM){f.push(new Ext.Action({text:"Delete",handler:function(){this.removeByRecord(a)},scope:this}))}var c=a.get("children");if(d==HST.CONTAINER&&c.length>0){f.push(new Ext.Action({text:"Delete items",handler:function(){var g="You are about to remove "+c.length+" items, are your sure?";Ext.Msg.confirm("Confirm delete",g,function(h,j){if(h=="yes"){var i=[c.length];Ext.each(c,function(k){i.push(b.getAt(b.findExact("id",k)))});Ext.each(i,b.remove,b)}})},scope:this}))}return f},removeByRecord:function(a){var b=this.stores.pageModel;Ext.Msg.confirm("Confirm delete","Are you sure you want to delete "+a.get("name")+"?",function(c,d){if(c=="yes"){b.remove(a)}})},removeByElement:function(c){var a=this.stores.pageModel;var b=a.findExact("id",Ext.fly(c).getAttribute("hst:id"));this.removeByRecord(a.getAt(b))},handleFrameMessages:function(a,c){try{if(c.tag=="rearrange"){this.onRearrangeContainer(c.data.id,c.data.children)}else{if(c.tag=="onclick"){this.handleOnClick(c.data.element)}else{if(c.tag=="receiveditem"){this.handleReceivedItem(c.data.id,c.data.element)}else{if(c.tag=="remove"){this.removeByElement(c.data.element)}else{if(c.tag=="onappload"){this.onIframeAppLoaded(c.data)}else{if(c.tag=="refresh"){this.refreshIframe()}}}}}}}catch(b){console.error(b)}}});Hippo.App.RestStore=Ext.extend(Ext.data.Store,{constructor:function(c){var a=new Ext.data.JsonReader({successProperty:"success",root:"data",messageProperty:"message",idProperty:"id"},c.prototypeRecord);var d=new Ext.data.JsonWriter({encode:false});var b={restful:true,reader:a,writer:d};Ext.apply(this,b,c);Hippo.App.RestStore.superclass.constructor.call(this,c)}});Hippo.App.ToolkitStore=Ext.extend(Hippo.App.RestStore,{constructor:function(b){var c=new Ext.data.HttpProxy({api:{read:"_rp/"+b.toolkitId+"./toolkit",create:"#",update:"#",destroy:"#"}});var a={id:"ToolkitStore",proxy:c,prototypeRecord:Hippo.App.PageModel.ReadRecord};Ext.apply(b,a);Hippo.App.ToolkitStore.superclass.constructor.call(this,b)}});Hippo.App.PageModelStore=Ext.extend(Hippo.App.RestStore,{constructor:function(b){var c=new Ext.data.HttpProxy({api:{read:"_rp/"+b.pageId+"./pagemodel",create:{url:"_rp/PageModelService/create",method:"POST"},update:{url:"_rp/PageModelService/update",method:"POST"},destroy:{url:"_rp/PageModelService/destroy",method:"GET"}},listeners:{beforeload:{fn:function(d,e){Ext.Msg.wait("Loading page ...")}},beforewrite:{fn:function(e,g,d,h){Ext.Msg.wait("Updating configuration ... ");if(g=="create"){var f=d.get("id");var j=d.get("parentId");e.setApi(g,{url:"_rp/"+j+"./create/"+f,method:"POST"})}else{if(g=="update"){var i=d.get("id");e.setApi(g,{url:"_rp/"+i+"./update",method:"POST"})}else{if(g=="destroy"){var j=d.get("parentId");e.setApi(g,{url:"_rp/"+j+"./delete",method:"GET"})}}}}},write:{fn:function(f,i,d,g,e){Ext.Msg.hide();Ext.Msg.wait("Refreshing page ...");var h=Ext.getCmp("Iframe");h.getFrameDocument().location.reload(true)}},load:{fn:function(e,d,f){Ext.Msg.hide()}}}});var a={id:"PageModelStore",proxy:c,prototypeRecord:Hippo.App.PageModel.ReadRecord};Ext.apply(b,a);Hippo.App.PageModelStore.superclass.constructor.call(this,b)}});Hippo.App.DragDropOne=(function(){return{init:function(a){a.onRender=a.onRender.createSequence(this.onRender)},onRender:function(){var c=Ext.getCmp("Iframe");var d=c.getFrame();this.boxs=[];this.nodeOverRecord=null;var b=this;this.dragZone=new Ext.grid.GridDragZone(this,{containerScroll:true,ddGroup:"blabla",onInitDrag:function(){var f=Ext.getCmp("Iframe");var e=f.getFrameDocument();f.getFrame().sendMessage({groups:"dropzone"},"highlight");Hippo.App.Main.stores.pageModel.each(function(g){var i=g.get("type");if(g.get("type")===HST.CONTAINER){var k=g.get("id")+"-overlay";var h=e.getElementById(k);var j=Ext.Element.fly(h).getBox();b.boxs.push({record:g,box:j})}});Ext.ux.ManagedIFrame.Manager.showShims()},onEndDrag:function(){b.boxs=[];Ext.ux.ManagedIFrame.Manager.hideShims();Ext.getCmp("Iframe").getFrame().sendMessage({groups:"dropzone"},"unhighlight")}});var a=this;this.dropZone=new Ext.dd.DropZone(c.body.dom,{ddGroup:"blabla",getTargetFromEvent:function(f){return f.getTarget()},onNodeOver:function(l,n,m,h){var g=n.lastPageX+n.deltaX;var f=n.lastPageY+n.deltaY;f-=27;for(var j=0;j<b.boxs.length;j++){var o=b.boxs[j],k=o.box;if(g>=k.x&&g<=k.right&&f>=k.y&&f<=k.bottom){b.nodeOverRecord=o.record;return Ext.dd.DropZone.prototype.dropAllowed}}b.nodeOverRecord=null;return Ext.dd.DropZone.prototype.dropNotAllowed},onNodeDrop:function(r,v,s,m){if(b.nodeOverRecord!=null){var g=a.getSelectionModel().getSelections();var j=Ext.getCmp("PageModelGrid");var u=b.nodeOverRecord;var t=j.getStore();var p=u.get("id");var f=[];var k=u.data.children.length+1;var h=t.indexOf(u)+k;for(var l=0;l<g.length;l++){var o=g[l];var q={parentId:p,id:o.get("id"),name:null,type:HST.CONTAINERITEM,template:o.get("template"),componentClassName:o.get("componentClassName"),xtype:o.get("xtype")};var n=Hippo.App.PageModel.Factory.createModel(null,q);f.push(n);t.insert(h+l,Hippo.App.PageModel.Factory.createRecord(n))}return true}return false}})}}})();