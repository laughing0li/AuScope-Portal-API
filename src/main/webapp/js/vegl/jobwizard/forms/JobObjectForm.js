/**
 * Job wizard form for creating and editing a new Job Object
 *
 * Author - Josh Vote
 */

Ext.define('vegl.jobwizard.forms.JobObjectForm', {
    extend : 'vegl.jobwizard.forms.BaseJobWizardForm',

    imageStore : null,
    computeTypeStore : null,

    /**
     * Creates a new JobObjectForm form configured to write/read to the specified global state
     */
    constructor: function(wizardState) {
        var jobObjectFrm = this;

        this.createSeries(wizardState);

        this.imageStore = Ext.create('Ext.data.Store', {
            model: 'vegl.models.MachineImage',
            proxy: {
                type: 'ajax',
                url: 'getVmImagesForComputeService.do',
                reader: {
                   type: 'json',
                   rootProperty : 'data'
                },
                extraParams: {
                    computeServiceId: 'aws-ec2-compute' //See ANVGL-35
                }
            }
        });
        this.imageStore.load();

        this.computeTypeStore = Ext.create('Ext.data.Store', {
            model: 'vegl.models.ComputeType',
            proxy: {
                type: 'ajax',
                url: 'getVmTypesForComputeService.do',
                reader: {
                   type: 'json',
                   rootProperty : 'data'
                }
            }
        });

        this.callParent([{
            wizardState : wizardState,
            bodyStyle: 'padding:10px;',
            header : false,
            defaults: { anchor: "100%" },
            labelWidth: 150,
            autoScroll: true,
            listeners : {
                //The first time this form is active create a new job object
                jobWizardActive : function() {
                    //If we have a jobId, load that, OTHERWISE the job will be created later
                    if (jobObjectFrm.wizardState.jobId) {
                        jobObjectFrm.getForm().load({
                            url : 'getJobObject.do',
                            waitMsg : 'Loading Job Object...',
                            params : {
                                jobId : jobObjectFrm.wizardState.jobId
                            },
                            failure : Ext.bind(jobObjectFrm.fireEvent, jobObjectFrm, ['jobWizardLoadException']),
                            success : function(frm, action) {
                                var responseObj = Ext.JSON.decode(action.response.responseText);

                                if (responseObj.success) {
                                    //Loads the image store of user selected compute provider
                                    frm.setValues(responseObj.data[0]);
                                    jobObjectFrm.wizardState.jobId = frm.getValues().id;
                                }
                            }
                        });
                    }
                }
            },
            fieldDefaults: {
                labelWidth: 120
            },
            items: [{
                xtype: 'textfield',
                name: 'name',
                itemId : 'name',
                fieldLabel: 'Job Name',
                value : Ext.util.Format.format('ANVGL Job - {0}', Ext.Date.format(new Date(), 'Y-m-d g:i a')),
                plugins: [{
                    ptype: 'fieldhelptext',
                    text: 'Enter a useful name for your job here.'
                }],
                allowBlank: true
            },{
                xtype: 'textfield',
                name: 'description',
                itemId : 'description',
                fieldLabel: 'Job Description',
                plugins: [{
                    ptype: 'fieldhelptext',
                    text: 'Enter an optional description for your job here.'
                }],
                allowBlank: true
            },{
                //See ANVGL-35
                xtype : 'hiddenfield',
                itemId: 'computeServiceId',
                name: 'computeServiceId',
                value: 'aws-ec2-compute'
            },{
                //See ANVGL-35
                xtype : 'hiddenfield',
                itemId: 'storageServiceId',
                name: 'storageServiceId',
                value: 'amazon-aws-storage-sydney'
            },{
                xtype : 'machineimagecombo',
                fieldLabel : 'Toolbox',
                name: 'computeVmId',
                itemId : 'image-combo',
                allowBlank: false,
                queryMode: 'local',
                triggerAction: 'all',
                typeAhead: true,
                forceSelection: true,
                store : this.imageStore,
                listConfig : {
                    loadingText: 'Getting tools...',
                    emptyText: 'No matching toolboxes found. Select a different compute location.'
                },
                plugins: [{
                    ptype: 'fieldhelptext',
                    text: 'Select a toolbox that contains software that you would like to use to process your data.'
                }],
                listeners : {
                    select : Ext.bind(this.onImageSelect, this)
                }
            },{
                xtype : 'combo',
                fieldLabel : 'Resources',
                name: 'computeTypeId',
                itemId : 'resource-combo',
                displayField : 'longDescription',
                valueField : 'id',
                allowBlank: false,
                queryMode: 'local',
                triggerAction: 'all',
                typeAhead: true,
                forceSelection: true,
                store : this.computeTypeStore,
                listConfig : {
                    loadingText: 'Getting available resources...',
                    emptyText: 'No matching resource configurations found. Select a different compute location.'
                },
                plugins: [{
                    ptype: 'fieldhelptext',
                    text: 'Select a compute resource configuration that is sufficient for your needs.'
                }]
            },{
                xtype : 'checkboxfield',
                fieldLabel : 'Email Notification',
                name: 'emailNotification',
                itemId : 'emailNotification',
                checked: true,
                plugins: [{
                    ptype: 'fieldhelptext',
                    text: 'Tick to receive email notification upon job processing.'
                }]
            },
            { xtype: 'hidden', name: 'id' },
            { xtype: 'hidden', name: 'storageProvider' },
            { xtype: 'hidden', name: 'storageEndpoint' },
            { xtype: 'hidden', name: 'registeredUrl' },
            { xtype: 'hidden', name: 'vmSubsetFilePath' },
            { xtype: 'hidden', name: 'vmSubsetUrl' }
            ]
        }]);
    },

    loadImages : function() {
        this.getComponent('image-combo').clearValue();
        this.getComponent('resource-combo').clearValue();
        this.imageStore.load({
            params : {
                computeServiceId : 'aws-ec2-compute' //See ANVGl-35
            }
        });
    },

    onImageSelect : function(combo, records) {
        if (!records) {
            this.computeTypeStore.removeAll();
            return;
        }

        this.getComponent('resource-combo').clearValue();
        var selectedComputeService = this.getComponent('computeServiceId').getValue();
        this.computeTypeStore.load({
            params : {
                computeServiceId : selectedComputeService,
                machineImageId : records.get('imageId')
            }
        });
    },

    getTitle : function() {
        return "Enter job details...";
    },

    getNumDownloadRequests : function() {
        request = ((window.XMLHttpRequest) ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP"));
        request.open("GET", "getNumDownloadRequests.do", false); //<-- false makes it a synchonous request!
        request.send(null);
        respObj = Ext.JSON.decode(request.responseText);
        size = respObj.data;
        return size;
    },

    beginValidation : function(callback) {
        var jobObjectFrm = this;
        var wizardState = this.wizardState;

        var numDownloadReqs = this.getNumDownloadRequests();



        //Ensure we have entered all appropriate fields
        if (!jobObjectFrm.getForm().isValid()) {
            callback(false);
            return;
        }

        //Then save the job to the database before proceeding
        var values = jobObjectFrm.getForm().getValues();
        values.seriesId = jobObjectFrm.wizardState.seriesId;
        Ext.Ajax.request({
            url : 'updateOrCreateJob.do',
            params : values,
            callback : function(options, success, response) {
                if (!success) {
                    portal.widgets.window.ErrorWindow.showText('Error saving details', 'There was an unexpected error when attempting to save the details on this form. Please try again in a few minutes.');
                    callback(false);
                    return;
                }

                var responseObj = Ext.JSON.decode(response.responseText);
                if (!responseObj.success) {
                    portal.widgets.window.ErrorWindow.showText('Error saving details', 'There was an unexpected error when attempting to save the details on this form.', responseObj.msg);
                    callback(false);
                    return;
                }

                jobObjectFrm.wizardState.jobId = responseObj.data[0].id;
                // Store user selected toolbox into wizard state. That toolbox
                // will be used to select relevant script templates or examples.
                wizardState.toolbox = jobObjectFrm.getForm().findField("computeVmId").getRawValue();

                // Store selected resource limits into wizard state. These values will be included
                // in template generation (to ensure valid numbers of CPU's are chosen etc)
                var computeTypeId = jobObjectFrm.getComponent('resource-combo').getValue();
                var computeType = jobObjectFrm.computeTypeStore.getById(computeTypeId);
                wizardState.ncpus = computeType.get('vcpus');
                wizardState.nrammb = computeType.get('ramMB');

                if (!wizardState.skipConfirmPopup && numDownloadReqs === 0) {
                    Ext.Msg.confirm('Confirm',
                            'No data set has been captured. Do you want to continue?',
                            function(button) {
                                if (button === 'yes') {
                                    wizardState.skipConfirmPopup = true;
                                    callback(true);
                                    return;
                                } else {
                                    callback(false);
                                    return;
                                }
                        });
                } else {
                    callback(true);
                    return;
                }

            }
        });
    },

    createSeries : function(wizardState) {

        Ext.Ajax.request({
            url: 'secure/createSeries.do',
            callback : function(options, success, response) {
                if (success) {
                    var responseObj = Ext.JSON.decode(response.responseText);
                    if (responseObj.success && Ext.isNumber(responseObj.data[0].id)) {
                        wizardState.seriesId = responseObj.data[0].id;
                        return;
                    } else {
                        errorMsg = responseObj.msg;
                        errorInfo = responseObj.debugInfo;
                    }
                } else {
                    errorMsg = "There was an internal error saving your series.";
                    errorInfo = "Please try again in a few minutes or report this error to cg_admin@csiro.au.";
                }

                portal.widgets.window.ErrorWindow.showText('Create new series', errorMsg, errorInfo);

                return;
            }
        });

    },

    getHelpInstructions : function() {
        var name = this.getComponent('name');
        var description = this.getComponent('description');
        var toolbox = this.getComponent('image-combo');
        var emailNotification = this.getComponent('emailNotification');

        return [Ext.create('portal.util.help.Instruction', {
            highlightEl : name.getEl(),
            title : 'Name your job',
            anchor : 'bottom',
            description : 'Every job requires a name. Names don\'t have to be unique but it\'s recommended you choose something meaningful as it will be the primary way to identify this job in the future.'
        }), Ext.create('portal.util.help.Instruction', {
            highlightEl : description.getEl(),
            title : 'Describe your job',
            anchor : 'bottom',
            description : 'Enter an optional description for your job here.'
        }), Ext.create('portal.util.help.Instruction', {
            highlightEl : toolbox.getEl(),
            title : 'Storage Toolbox',
            anchor : 'bottom',
            description : 'A toolbox is a collection of software packages that will be made available to your job when it starts processing. Some toolboxes are restricted to authorised users for licensing reasons. You will not be able to choose a toolbox until after you select a compute provider.'
        }), Ext.create('portal.util.help.Instruction', {
            highlightEl : emailNotification.getEl(),
            title : 'Job completion email notification',
            anchor : 'bottom',
            description : 'The VL will send out email notification to your email address upon job completion. Untick the checkbox if you don\'t want to receive the notification.'
        })];
    }
});