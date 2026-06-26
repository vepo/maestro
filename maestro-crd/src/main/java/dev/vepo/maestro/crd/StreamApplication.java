package dev.vepo.maestro.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("maestro.dev")
@Version("v1alpha1")
@Plural("streamapplications")
public class StreamApplication extends CustomResource<StreamApplicationSpec, StreamApplicationStatus> implements Namespaced {}
