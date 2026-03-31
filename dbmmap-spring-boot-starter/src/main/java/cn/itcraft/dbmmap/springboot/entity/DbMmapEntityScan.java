package cn.itcraft.dbmmap.springboot.entity;

import cn.itcraft.dbmmap.core.mapping.MapperRegistry;
import cn.itcraft.dbmmap.core.type.TypeHandlerRegistry;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.lang.annotation.*;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DbMmapEntityScan.Registrar.class)
public @interface DbMmapEntityScan {
    String[] value() default {};
    String[] basePackages() default {};
    Class<?>[] basePackageClasses() default {};

    class Registrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            String[] basePackages = getBasePackages(importingClassMetadata);

            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(DbMmapEntity.class));

            for (String basePackage : basePackages) {
                Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
                for (BeanDefinition candidateComponent : candidateComponents) {
                    String className = candidateComponent.getBeanClassName();
                    try {
                        Class<?> entityClass = Class.forName(className);
                        DbMmapEntity annotation = entityClass.getAnnotation(DbMmapEntity.class);
                        System.out.println("[dbmmap] Registered entity: " + entityClass.getSimpleName());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load entity class: " + className, e);
                    }
                }
            }
        }

        private String[] getBasePackages(AnnotationMetadata importingClassMetadata) {
            String[] value = (String[]) importingClassMetadata.getAnnotationAttributes(DbMmapEntityScan.class.getName()).get("value");
            String[] basePackages = (String[]) importingClassMetadata.getAnnotationAttributes(DbMmapEntityScan.class.getName()).get("basePackages");
            Class<?>[] basePackageClasses = (Class<?>[]) importingClassMetadata.getAnnotationAttributes(DbMmapEntityScan.class.getName()).get("basePackageClasses");

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