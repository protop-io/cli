package io.protop.core.manifest;

import io.protop.core.error.ServiceException;
import io.protop.core.error.ServiceExceptionConsumer;

public class InvalidDependencyName extends ServiceException {

    InvalidDependencyName(String name) {
        super(String.format("Invalid dependency name: %s.", name));
    }

    @Override
    public void accept(ServiceExceptionConsumer consumer) {
        consumer.consume(this);
    }
}
