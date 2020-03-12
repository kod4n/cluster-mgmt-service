package io.cratekube.clustermgmt.modules.annotation

import com.google.inject.BindingAnnotation

import java.lang.annotation.Retention
import java.lang.annotation.Target

import static java.lang.annotation.ElementType.FIELD
import static java.lang.annotation.ElementType.METHOD
import static java.lang.annotation.ElementType.PARAMETER
import static java.lang.annotation.RetentionPolicy.RUNTIME

@BindingAnnotation
@Target([FIELD, PARAMETER, METHOD])
@Retention(RUNTIME)
@interface ClusterCache {}

@BindingAnnotation
@Target([FIELD, PARAMETER, METHOD])
@Retention(RUNTIME)
@interface ClusterExecutor {}

@BindingAnnotation
@Target([FIELD, PARAMETER, METHOD])
@Retention(RUNTIME)
@interface ClusterProcessExecutor {}

@BindingAnnotation
@Target([FIELD, PARAMETER, METHOD])
@Retention(RUNTIME)
@interface ManagedResourceCache {}

@BindingAnnotation
@Target([FIELD, PARAMETER, METHOD])
@Retention(RUNTIME)
@interface ManagedResourceExecutor {}

@BindingAnnotation
@Target([FIELD, PARAMETER, METHOD])
@Retention(RUNTIME)
@interface ManagedResourceProcessExecutor {}
