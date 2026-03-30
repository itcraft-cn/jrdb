package com.dbmmap.springboot.repository;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.lang.annotation.*;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DbMmapRepositoryScan.Registrar.class)
public @interface DbMmapRepositoryScan {
    String[] value() default {};
    String[] basePackages() default {};
    Class<?>[] basePackageClasses() default {};

    class Registrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

        private ResourceLoader resourceLoader;
        private Environment environment;

        @Override
        public void setResourceLoader(ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }

        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
                @Override
                protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                    return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
                }
            };
            scanner.addIncludeFilter(new AnnotationTypeFilter(DbMmapRepository.class));
            scanner.setResourceLoader(this.resourceLoader);

            String[] basePackages = getBasePackages(importingClassMetadata);

            for (String basePackage : basePackages) {
                Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
                for (BeanDefinition candidateComponent : candidateComponents) {
                    if (candidateComponent instanceof AnnotatedBeanDefinition) {
                        AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                        AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                        String interfaceName = annotationMetadata.getClassName();

                        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DbMmapRepositoryFactoryBean.class);
                        builder.addPropertyValue("interface", interfaceName);
                        builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

                        AbstractBeanDefinition definition = builder.getBeanDefinition();
                        String beanName = BeanDefinitionReaderUtils.generateBeanName(definition, registry);
                        registry.registerBeanDefinition(beanName, definition);
                    }
                }
            }
        }

        private String[] getBasePackages(AnnotationMetadata importingClassMetadata) {
            String[] value = (String[]) importingClassMetadata.getAnnotationAttributes(DbMmapRepositoryScan.class.getName()).get("value");
            String[] basePackages = (String[]) importingClassMetadata.getAnnotationAttributes(DbMmapRepositoryScan.class.getName()).get("basePackages");
            Class<?>[] basePackageClasses = (Class<?>[]) importingClassMetadata.getAnnotationAttributes(DbMmapRepositoryScan.class.getName()).get("basePackageClasses");

            if (value.length == 0 && basePackages.length == 0 && basePackageClasses.length == 0) {
                return new String[] { ClassUtils.getPackageName(importingClassMetadata.getClassName()) };
            }

            String[] packages = new String[value.length + basePackages.length + basePackageClasses.length];
            System.arraycopy(value, 0, packages, 0, value.length);
            System.arraycopy(basePackages, 0, packages, value.length, basePackages.length);
            for (int i = 0; i < basePackageClasses.length; i++) {
                packages[value.length + basePackages.length + i] = ClassUtils.getPackageName(basePackageClasses[i]);
            }
            return packages;
        }
    }
}