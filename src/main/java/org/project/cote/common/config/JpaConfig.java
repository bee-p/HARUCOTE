package org.project.cote.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 관련 설정.
 * <p>{@code @EnableJpaAuditing} 을 {@link org.project.cote.CoteApplication} 에 두면
 * {@code @WebMvcTest} 같은 슬라이스 테스트에서 JPA metamodel 초기화를 강제해 컨텍스트 로딩이 실패한다.
 * 별도 {@code @Configuration} 으로 분리하면 슬라이스 테스트는 이 빈을 로드하지 않는다.</p>
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
